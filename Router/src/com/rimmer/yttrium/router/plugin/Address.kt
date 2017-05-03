package com.rimmer.yttrium.router.plugin

import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.RouteModifier
import com.rimmer.yttrium.router.RouteProperty
import java.net.InetSocketAddress

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
        val remote = route.requestHeaders?.get("X-Forwarded-For") ?:
            (route.channel.channel().remoteAddress() as? InetSocketAddress)?.hostName ?: ""

        route.parameters[context] = IPAddress(remote)
        f(null)
    }
}