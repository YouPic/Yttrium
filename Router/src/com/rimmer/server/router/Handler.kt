package com.rimmer.server.router

import com.rimmer.server.InvalidStateException
import com.rimmer.server.NotFoundException
import com.rimmer.server.UnauthorizedException
import com.rimmer.server.serialize.writeJson
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class RouteHandler(
    val route: Route,
    val plugins: List<RoutePlugin>,
    val instance: Any?,
    val function: KFunction<*>,
    val self: KParameter?,
    val displayName: String,
    val listener: RouteListener?
) {
    /** The listener id of this call. We save this here to avoid allocating large closures. */
    var listenerId = 0L

    /**
     * Handles an HTTP request.
     * @param channel The channel this request arrived from.
     * @param pathParams A list of path parameters.
     * The list contains one value for each path parameter in the route.
     * @param queryParams A list of query parameters.
     * The list contains one value for each query parameter in the route.
     * Optional parameters may be null.
     */
    fun handle(channel: ChannelHandlerContext, pathParams: List<Any>, queryParams: Array<Any?>, f: (HttpResponse) -> Unit) {
        if(listener != null) {
            listenerId = listener.onStart(displayName, route)
        }

        val response = try {
            val arguments = createParameters(pathParams, queryParams)

            // Add the this-instance for non-static functions.
            self?.let {
                arguments.put(it, instance)
            }

            // Let each plugin modify the call as needed.
            for(p in plugins) {
                p.plugin.modifyCall(p.context, channel.channel().remoteAddress(), pathParams, p.firstPath, queryParams, p.firstQuery, arguments)
            }

            // Call the actual api function and return its result.
            // Since we call it through reflection, any exceptions are wrapped in
            // InvocationTargetException. We rethrow the original exception so everything works correctly.
            var result = try {
                function.callBy(arguments)
            } catch(e: InvocationTargetException) {
                if (e.targetException != null) {
                    throw e.targetException
                } else {
                    throw e
                }
            }

            // Let each plugin modify the result as needed.
            for(p in plugins) {
                result = p.plugin.modifyResult(p.context, pathParams, p.firstPath, queryParams, p.firstQuery, result)
            }

            // Write the final result to a buffer.
            val finalResult = result
            val buffer = channel.alloc().buffer()
            writeJson(finalResult, buffer)

            // Create the http response.
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer)
            response.headers().add(HttpHeaderNames.CONTENT_TYPE, jsonContentType)

            listener?.onSucceed(listenerId)
            response
        } catch(e: InvalidStateException) {
            // InvalidState is thrown whenever something was wrong with the request.
            // We map it to 400 Bad Request.
            val reason = e.message ?: "bad_request"
            val r = errorResponse(HttpResponseStatus.BAD_REQUEST, reason)
            listener?.onFail(listenerId, false, e)
            r
        } catch(e: UnauthorizedException) {
            // Unauthorized is thrown whenever the provided session doesn't have the correct permissions.
            // We map it to 403 Forbidden, since 401 would require a possibility for authorization in the response.
            val reason = e.message ?: "forbidden"
            val r = errorResponse(HttpResponseStatus.FORBIDDEN, reason)
            listener?.onFail(listenerId, false, e)
            r
        } catch(e: NotFoundException) {
            // NotFound is thrown whenever a requested item doesn't exist. We map it to 404.
            val reason = e.message ?: "not_found"
            val r = errorResponse(HttpResponseStatus.NOT_FOUND, reason)
            listener?.onFail(listenerId, false, e)
            r
        } catch(e: Throwable) {
            // We assume that any other exception means something went wrong in the code,
            // and return an internal error.
            val error = StringWriter()
            e.printStackTrace(PrintWriter(error))
            val r = errorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "internal_error")
            listener?.onFail(listenerId, true, e)
            r
        }

        // Send the response to the callback.
        f(response)
    }

    /**
     * Creates a parameter map for calling a specific routing function.
     * @param pathParams The provided path parameters for this request.
     * @param queryParams The provided query parameters for this request.
     * @return A map from function parameter to parsed object.
     */
    private fun createParameters(pathParams: List<Any>, queryParams: Array<Any?>): MutableMap<KParameter, Any?> {
        // This must be a hash map, since KParameter doesn't implement Comparable.
        val map = HashMap<KParameter, Any?>()

        // Retrieve each path parameter and parse it as json. Path parameters are always required.
        // We only handle parameters that have an associated KParameter, since the others were added by plugins.
        var paramIndex = 0
        val paramCount = pathParams.size
        while(paramIndex < paramCount) {
            val parameter = route.pathParams[paramIndex]
            if(parameter.parameter !== null) {
                map.put(parameter.parameter, pathParams[paramIndex])
            }
            paramIndex++
        }

        // Retrieve each query parameter and parse it as json. Query parameters that have a default value are optional.
        // We only handle parameters that have an associated KParameter, since the others were added by plugins.
        var queryIndex = 0
        var queryCount = queryParams.size
        while(queryIndex < queryCount) {
            val query = queryParams[queryIndex]
            val routeQuery = route.queryParams[queryIndex]
            if(query !== null && routeQuery.parameter !== null) {
                map.put(routeQuery.parameter, query)
            } else if(routeQuery.required) {
                throw InvalidStateException("This path requires a query parameter ${routeQuery.name}")
            }
            queryIndex++
        }
        return map
    }
}

/** Creates an error response with the provided error code and text. */
private fun errorResponse(error: HttpResponseStatus, text: String): FullHttpResponse {
    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, error, Unpooled.wrappedBuffer(text.toByteArray()))
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
    return response
}

/** Cached content type string for less conversions. */
val jsonContentType = AsciiString("application/json")