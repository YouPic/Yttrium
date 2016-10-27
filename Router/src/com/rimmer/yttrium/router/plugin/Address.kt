package com.rimmer.yttrium.router.plugin

import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.RouteModifier
import com.rimmer.yttrium.router.RouteProperty
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.reflect.KParameter

/** Functions that have a parameter of this type will receive the id-address of the caller. */
class IPAddress(val ip: String)

/** Plugin for sending the caller ip-address to routes. */
class AddressPlugin: Plugin<Int> {
    override val name = "AddressPlugin"

    override fun modifyRoute(modifier: RouteModifier, properties: List<RouteProperty>): Int {
        return modifier.provideFunArg(IPAddress::class.java) ?:
            throw IllegalArgumentException("No parameter of type IPAddress exists.")
    }

    override fun modifyCall(context: Int, route: RouteContext, f: (Throwable?) -> Unit) {
        route.parameters[context] = IPAddress(route.sourceIp)
        f(null)
    }
}