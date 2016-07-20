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
    crossinline getStats: RouteContext.(from: Long, to: Long) -> Task<StatResponse>,
    crossinline getProfile: RouteContext.(from: Long, to: Long) -> Task<ProfileResponse>,
    crossinline getError: RouteContext.(from: Long) -> Task<ErrorResponse>
) {
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("stats", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader, longReader), 
        null, 
        { getStats(it[0] as Long, it[1] as Long) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("profile", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader, longReader), 
        null, 
        { getProfile(it[0] as Long, it[1] as Long) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("error", null), PathSegment("from", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader), 
        null, 
        { getError(it[0] as Long) }
    )
}
