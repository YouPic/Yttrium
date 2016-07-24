package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.*
import com.rimmer.yttrium.router.*
import com.rimmer.yttrium.router.HttpMethod
import com.rimmer.yttrium.router.listener.RouteListener
import com.rimmer.yttrium.serialize.*
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.HttpData
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.util.AsciiString
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.*

val jsonContentType = AsciiString("application/json")

class HttpRouter(
    val router: Router,
    val listener: RouteListener? = null,
    val defaultHandler: (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit = ::httpDefault
): (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit {
    private val segmentTrees = HttpMethod.values().map { m ->
        buildSegments(router.routes.filter { it.method == m })
    }

    override fun invoke(context: ChannelHandlerContext, request: FullHttpRequest, f: (HttpResponse) -> Unit) {
        // Check if the request contains a version request.
        val acceptVersion = request.headers().get(HttpHeaderNames.ACCEPT)?.let {maybeParseInt(it)}
        val version = acceptVersion ?: request.headers().get("API-VERSION")?.let { maybeParseInt(it) } ?: 0
        val remote = request.headers().get("X-Forwarded-For") ?:
            (context.channel().remoteAddress() as? InetSocketAddress)?.hostName ?: ""

        // Check if the requested http method is known.
        val method = convertMethod(request.method())
        if(method == null) {
            defaultHandler(context, request, f)
            return
        }

        // Find a matching route and parse its path parameters.
        val parameters = ArrayList<String>()
        val route = findRoute(segmentTrees[method.ordinal], parameters, version, request.uri(), 1)
        if(route == null) {
            defaultHandler(context, request, f)
            return
        }

        val eventLoop = context.channel().eventLoop()
        val callId = listener?.onStart(eventLoop, route) ?: 0
        val fail = {r: RouteContext, e: Throwable? ->
            f(mapError(e))
            listener?.onFail(r, e)
        }

        try {
            val params = parseParameters(route, parameters)
            val queries = parseQuery(route, request.uri())
            val content = request.content()
            val contentStart = content.readerIndex()
            val bodyHandler = route.bodyQuery

            // Parse any parameters that were provided through the request body.
            // Only parse as form-data if the whole body isn't
            val parseError = if(bodyHandler == null) {
                if (request.headers()[HttpHeaderNames.CONTENT_TYPE] == "application/json") {
                    parseJsonBody(route, request, queries)
                } else if (route.bodyQuery == null) {
                    parseBodyQuery(route, request, queries)
                } else null
            } else {
                queries[bodyHandler] = BodyContent(content.readerIndex(contentStart))
                null
            }

            // Make sure all required parameters were provided, and handle optional ones.
            checkQueries(route, queries, parseError)

            // Call the route with a listener that sends the result back to the client.
            val listener = object: RouteListener {
                override fun onStart(eventLoop: EventLoop, route: Route) = 0L
                override fun onSucceed(route: RouteContext, result: Any?) {
                    val buffer = context.alloc().buffer()
                    try {
                        writeJson(result, route.route.writer, buffer)
                        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer)
                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, jsonContentType)
                        f(response)
                        listener?.onSucceed(route, result)
                    } catch(e: Throwable) { fail(route, e) }
                }
                override fun onFail(route: RouteContext, reason: Throwable?) { fail(route, reason) }
            }

            route.handler(RouteContext(context, remote, eventLoop, route, params, queries, callId), listener)
        } catch(e: Throwable) {
            // We don't have the call parameters here, so we just send a route context without them.
            fail(RouteContext(context, remote, eventLoop, route, emptyArray(), emptyArray(), callId), e)
        }
    }
}

private class HttpSegment(
    val localRoutes: Array<Route>,
    val localHashes: IntArray,
    val localWildcards: Array<Route>,
    val nextRoutes: Array<HttpSegment>,
    val nextHashes: IntArray,
    val nextWildcards: HttpSegment?
)

/**
 * Creates a tree of segment groups from a route list.
 * The segment endpoints within each leaf are descending-sorted by version,
 * which allows a searcher to take the first match.
 */
private fun buildSegments(routes: Iterable<Route>, segmentIndex: Int = 0): HttpSegment {
    val (endPoints, wildcardEndpoints) = routes.filter {
        it.segments.size == segmentIndex + 1
    }.sortedByDescending {
        it.version
    }.partition {
        it.segments[segmentIndex].reader === null
    }.run {
        first.toTypedArray() to second.toTypedArray()
    }

    val hashes = endPoints.map {
        val segment = it.segments[segmentIndex]
        segment.name.hashCode()
    }.toIntArray()

    val groups = routes.filter {
        it.segments.size > segmentIndex + 1 && it.segments[segmentIndex].reader === null
    }.groupBy {
        it.segments[segmentIndex].name
    }

    val next = groups.map { buildSegments(it.value, segmentIndex + 1) }.toTypedArray()
    val nextHashes = groups.map { it.key.hashCode() }.toIntArray()

    val wildcardRoutes = routes.filter {
        it.segments.size > segmentIndex + 1 && it.segments[segmentIndex].reader !== null
    }
    val wildcards = if(wildcardRoutes.size > 0) buildSegments(wildcardRoutes, segmentIndex + 1) else null

    return HttpSegment(endPoints, hashes, wildcardEndpoints, next, nextHashes, wildcards)
}

/**
 * Parses an HTTP request path and returns any matching route handler.
 * @param parameters A list of parameters that will be filled
 * with the path parameters of the returned route, in reverse order.
 */
private fun findRoute(segment: HttpSegment, parameters: ArrayList<String>, version: Int, url: String, start: Int): Route? {
    // Filter out any leading slashes.
    var segmentStart = start
    while(url.getOrNull(segmentStart) == '/') segmentStart++

    // Find the first url segment. This is used to create a set of possible methods.
    var segmentEnd = url.indexOf('/', segmentStart + 1)
    if(segmentEnd == -1) {
        segmentEnd = url.indexOf('?', segmentStart + 1)
        if(segmentEnd == -1) {
            segmentEnd = url.length
        }
    }

    val hash = url.sliceHash(segmentStart, segmentEnd)
    if(segmentEnd >= url.length || url[segmentEnd] == '?') {
        segment.localHashes.forEachIndexed { i, v ->
            val route = segment.localRoutes[i]
            if((v == hash) && route.version <= version) return route
        }

        segment.localWildcards.forEach {
            if(it.version <= version) {
                parameters.add(url.substring(segmentStart, segmentEnd))
                return it
            }
        }
        return null
    }

    val i = segment.nextHashes.indexOf(hash)
    val handler = if(i >= 0) findRoute(segment.nextRoutes[i], parameters, version, url, segmentEnd + 1) else null
    if(handler != null) return handler

    val wildcard = segment.nextWildcards?.let { findRoute(it, parameters, version, url, segmentEnd + 1) }
    if(wildcard != null) {
        parameters.add(url.substring(segmentStart, segmentEnd))
    }
    return wildcard
}

/**
 * Parses the parameter list returned by findHandler into the correct types.
 * @param parameters A reverse list of path parameters for this route.
 */
private fun parseParameters(route: Route, parameters: Iterable<String>): Array<Any?> {
    val length = route.typedSegments.size
    val array = arrayOfNulls<Any>(length)
    parameters.forEachIndexed { i, p ->
        val index = length - i - 1
        val string = URLDecoder.decode(p, "UTF-8")
        array[index] = readPrimitive(string, route.typedSegments[index].reader!!.target)
    }
    return array
}

/** Parses the query parameters for this route into `queries`. Returns an error string if the url is invalid. */
private fun parseQuery(route: Route, url: String): Array<Any?> {
    val params = route.queries
    val query = url.substringAfter('?', "")
    val values = arrayOfNulls<Any>(params.size)

    if(query.isNotEmpty()) {
        val queries = query.split('&')

        // Parse each query parameter.
        for(q in queries) {
            // Filter out any empty query parameters.
            if(q.length == 0) continue

            val separator = q.indexOf('=')
            if(separator == -1) {
                // Bad syntax.
                throw InvalidStateException("Invalid query syntax: expected '='")
            }

            // Check if this parameter is used.
            val name = q.sliceHash(0, separator)
            params.forEachIndexed { i, query ->
                if(query.hash == name && query.reader.target !== BodyContent::class.java) {
                    val string = URLDecoder.decode(q.substring(separator + 1), "UTF-8")
                    values[i] = readPrimitive(string, query.reader.target)
                }
            }
        }
    }

    return values
}

/**
 * Parses any query parameters that were provided through the request body.
 * @return The first parsing error that occurred, which can be propagated if the whole request fails due it.
 */
fun parseBodyQuery(route: Route, request: FullHttpRequest, queries: Array<Any?>): Throwable? {
    var error: Throwable? = null
    if(request.content().readableBytes() > 0) {
        val bodyDecoder = HttpPostRequestDecoder(request)
        while(try { bodyDecoder.hasNext() } catch(e: HttpPostRequestDecoder.EndOfDataDecoderException) { false }) {
            val p = bodyDecoder.next() as? HttpData ?: continue

            // Check if this parameter is recognized.
            val name = p.name.hashCode()
            route.queries.forEachIndexed { i, query ->
                if(query.hash == name && query.reader.target !== BodyContent::class.java) {
                    val buffer = p.byteBuf
                    val index = buffer.readerIndex()

                    // This is a bit ugly - we don't really know if the provided data is json or raw,
                    // so we just try parsing it in both ways.
                    try {
                        queries[i] = query.reader.fromJson(JsonToken(buffer))
                    } catch(e: Throwable) {
                        buffer.readerIndex(index)
                        try {
                            queries[i] = readPrimitive(buffer.string, query.reader.target)
                        } catch(e: Throwable) {
                            // If both parsing tries failed, we set the exception to be propagated if needed.
                            if(error == null) {
                                error = e
                            }
                        }
                    }
                }
            }
        }
    }
    return error
}

/** Parses query parameters from a json body. */
fun parseJsonBody(route: Route, request: FullHttpRequest, queries: Array<Any?>): Throwable? {
    val buffer = request.content()
    if(buffer.isReadable) {
        try {
            val json = JsonToken(buffer)
            json.expect(JsonToken.Type.StartObject)
            while(true) {
                json.parse()
                if(json.type == JsonToken.Type.EndObject) {
                    break
                } else if(json.type == JsonToken.Type.FieldName) {
                    val name = json.stringPayload.hashCode()
                    var found = false
                    route.queries.forEachIndexed { i, query ->
                        if(query.hash == name && query.reader.target !== BodyContent::class.java) {
                            found = true
                            val offset = buffer.readerIndex()

                            if(json.peekString()) {
                                // Sometimes body parameters are inside a json string, which we need to support.
                                // Since we can't know which one it is, we have to try both...
                                try {
                                    queries[i] = query.reader.fromJson(json)
                                } catch(e: Throwable) {
                                    buffer.readerIndex(offset)
                                    json.parse()
                                    queries[i] = query.reader.fromJson(JsonToken(json.stringPayload.byteBuf))
                                }
                            } else {
                                queries[i] = query.reader.fromJson(json)
                            }
                        }
                    }

                    if(!found) {
                        json.skipValue()
                    }
                } else {
                    return InvalidStateException("Expected json field name before offset ${request.content().readerIndex()}")
                }
            }
        } catch(e: Throwable) {
            return e
        }
    }

    return null
}

/** Makes sure that all required query parameters have been set correctly. */
fun checkQueries(route: Route, args: Array<Any?>, parseError: Throwable?) {
    route.queries.forEachIndexed { i, query ->
        val v = args[i]
        if(v == null) {
            if(query.optional) {
                args[i] = query.default
            } else {
                val description = if(query.description.isNotEmpty()) "(${query.description})" else "(no description)"
                val type = "of type ${query.reader.target.simpleName}"
                val error = if(parseError != null) "due to $parseError" else ""
                throw InvalidStateException(
                    "Request to ${route.name} is missing required query parameter \"${query.name}\" $description $type $error"
                )
            }
        }
    }
}