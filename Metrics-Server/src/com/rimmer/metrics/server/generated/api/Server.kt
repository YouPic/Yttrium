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

inline fun Router.serverApi(
    crossinline statistic: RouteContext.(stat: StatPacket) -> Task<Unit>,
    crossinline error: RouteContext.(error: ErrorPacket) -> Task<Unit>,
    crossinline profile: RouteContext.(profile: ProfilePacket) -> Task<Unit>
) {
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("statistic", null)), 
        listOf(BuilderQuery("stat", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(StatPacket.reader), 
        null, 
        { statistic(it[0] as StatPacket) }
    )
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("error", null)), 
        listOf(BuilderQuery("error", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(ErrorPacket.reader), 
        null, 
        { error(it[0] as ErrorPacket) }
    )
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("profile", null)), 
        listOf(BuilderQuery("profile", false, null, "")), 
        emptyList<Plugin<Any>>(), 
        arrayOf(ProfilePacket.reader), 
        null, 
        { profile(it[0] as ProfilePacket) }
    )
}
