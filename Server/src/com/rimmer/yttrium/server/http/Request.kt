package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.parseInt
import com.rimmer.yttrium.router.HttpMethod
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.Router
import com.rimmer.yttrium.sliceHash
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponse
import java.util.*

class HttpSegment(
    val routes: Array<Route>,
    val routeHashes: IntArray,
    val next: Array<HttpSegment>,
    val nextHashes: IntArray,
    val wildcard: HttpSegment?
)

/**
 * Creates a tree of segment groups from a route list.
 * The segment endpoints within each leaf are descending-sorted by version,
 * which allows a searcher to take the first match.
 */
fun buildSegments(routes: Iterable<Route>, segmentIndex: Int = 0): HttpSegment {
    val endPoints = routes.filter {
        it.segments.size == segmentIndex + 1
    }.sortedByDescending { it.version }.toTypedArray()

    val hashes = endPoints.map {
        val segment = it.segments[segmentIndex]
        if(segment.type == null) segment.name.hashCode() else -1
    }.toIntArray()

    val groups = routes.filter {
        it.segments.size > segmentIndex + 1 && it.segments[segmentIndex].type == null
    }.groupBy {
        it.segments[segmentIndex].name
    }

    val next = groups.map { buildSegments(it.value, segmentIndex + 1) }.toTypedArray()
    val nextHashes = groups.map { it.key.hashCode() }.toIntArray()

    val wildcardRoutes = routes.filter {
        it.segments.size > segmentIndex + 1 && it.segments[segmentIndex].type != null
    }
    val wildcards = if(wildcardRoutes.size > 0) buildSegments(wildcardRoutes, segmentIndex + 1) else null

    return HttpSegment(endPoints, hashes, next, nextHashes, wildcards)
}

fun findHandler(segment: HttpSegment, parameters: ArrayList<String>, version: Int, url: String, start: Int): Route? {
    // Find the first url segment. This is used to create a set of possible methods.
    val segmentStart = start
    var segmentEnd = url.indexOf('/', start + 1)
    if(segmentEnd == -1) {
        segmentEnd = url.indexOf('?', start + 1)
        if(segmentEnd == -1) {
            segmentEnd = url.length
        }
    }

    val hash = url.sliceHash(segmentStart, segmentEnd)
    if(segmentEnd >= url.length || url[segmentEnd] == '?') {
        segment.routeHashes.forEachIndexed { i, v ->
            val route = segment.routes[i]
            if((v == hash || v == -1) && route.version <= version) {
                if(v == -1) parameters.add(url.substring(segmentStart, segmentEnd))
                return route
            }
        }
        return null
    }

    val i = segment.nextHashes.indexOf(hash)
    val handler = if(i >= 0) findHandler(segment.next[i], parameters, version, url, segmentEnd + 1) else null
    if(handler != null) return handler

    val wildcard = segment.wildcard?.let { findHandler(it, parameters, version, url, segmentEnd + 1) }
    if(wildcard != null) {
        parameters.add(url.substring(segmentStart, segmentEnd))
    }
    return wildcard
}
