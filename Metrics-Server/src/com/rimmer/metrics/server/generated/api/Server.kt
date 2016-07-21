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

inline fun Router.serverApi(
    crossinline statistic: RouteContext.(stats: List<StatPacket>, ip: IPAddress) -> Task<Unit>,
    crossinline error: RouteContext.(errors: List<ErrorPacket>, ip: IPAddress) -> Task<Unit>,
    crossinline profile: RouteContext.(profiles: List<ProfilePacket>, ip: IPAddress) -> Task<Unit>
) {
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("statistic", null)), 
        listOf(BuilderQuery("stats", false, null, ""), BuilderQuery("ip", false, null, "")), 
        listOf(pluginMap["IPPlugin"]!!), 
        arrayOf(arrayReader<StatPacket>(StatPacket.reader), null), 
        null, 
        { statistic(it[0] as List<StatPacket>, it[1] as IPAddress) }
    )
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("error", null)), 
        listOf(BuilderQuery("errors", false, null, ""), BuilderQuery("ip", false, null, "")), 
        listOf(pluginMap["IPPlugin"]!!), 
        arrayOf(arrayReader<ErrorPacket>(ErrorPacket.reader), null), 
        null, 
        { error(it[0] as List<ErrorPacket>, it[1] as IPAddress) }
    )
    addRoute(
        HttpMethod.POST, 0, 
        emptyList<RouteProperty>(), 
        listOf(PathSegment("profile", null)), 
        listOf(BuilderQuery("profiles", false, null, ""), BuilderQuery("ip", false, null, "")), 
        listOf(pluginMap["IPPlugin"]!!), 
        arrayOf(arrayReader<ProfilePacket>(ProfilePacket.reader), null), 
        null, 
        { profile(it[0] as List<ProfilePacket>, it[1] as IPAddress) }
    )
}
