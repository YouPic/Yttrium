package com.rimmer.metrics.server.generated.api

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.router.*
import com.rimmer.yttrium.router.plugin.Plugin
import com.rimmer.metrics.server.generated.type.*

inline fun Router.clientApi(
    crossinline getStats: RouteContext.(from: Long, to: Long, password: String) -> Task<StatResponse>,
    crossinline getProfile: RouteContext.(from: Long, to: Long, password: String) -> Task<ProfileResponse>,
    crossinline getError: RouteContext.(from: Long, password: String) -> Task<ErrorResponse>
) {
    addRoute(
        HttpMethod.GET, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("stats", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        listOf(BuilderQuery("password", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(longReader, longReader, stringReader), 
        null, 
        { getStats(it[0] as Long, it[1] as Long, it[2] as String) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("profile", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        listOf(BuilderQuery("password", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(longReader, longReader, stringReader), 
        null, 
        { getProfile(it[0] as Long, it[1] as Long, it[2] as String) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("error", null), PathSegment("from", longReader)), 
        listOf(BuilderQuery("password", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(longReader, stringReader), 
        null, 
        { getError(it[0] as Long, it[1] as String) }
    )
}
