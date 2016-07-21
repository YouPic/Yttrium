package com.rimmer.metrics.server.generated.api

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.router.*
import com.rimmer.yttrium.router.plugin.Plugin
import com.rimmer.metrics.server.generated.type.*

inline fun Router.clientApi(
    crossinline getStats: RouteContext.(from: Long, to: Long) -> Task<List<TimeMetric>>,
    crossinline getProfile: RouteContext.(from: Long, to: Long) -> Task<List<TimeProfile>>,
    crossinline getError: RouteContext.(from: Long) -> Task<List<ErrorClass>>
) {
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("stats", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader, longReader), 
        arrayWriter<TimeMetric>(null), 
        { getStats(it[0] as Long, it[1] as Long) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("profile", null), PathSegment("from", longReader), PathSegment("to", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader, longReader), 
        arrayWriter<TimeProfile>(null), 
        { getProfile(it[0] as Long, it[1] as Long) }
    )
    addRoute(
        HttpMethod.GET, 0, 
        listOf(RouteProperty("password", true)), 
        listOf(PathSegment("error", null), PathSegment("from", longReader)), 
        emptyList<BuilderQuery>(), 
        listOf(pluginMap["PasswordPlugin"]!!), 
        arrayOf(longReader), 
        arrayWriter<ErrorClass>(null), 
        { getError(it[0] as Long) }
    )
}
