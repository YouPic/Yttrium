package com.rimmer.server.router.plugin

import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.reflect.KParameter

/** Functions that have a parameter of this type will receive the id-address of the caller. */
class IPAddress(val ip: String)

class AddressPlugin: Plugin<KParameter> {
    override fun isUsed(
        method: Annotation, parameters: MutableList<KParameter>, annotations: MutableList<Annotation>, returnType: Type
    ) = findParameter<IPAddress>(parameters)

    override fun modifyCall(
        context: KParameter,
        remote: SocketAddress,
        pathParams: List<Any>,
        firstPath: Int,
        queryParams: Array<Any?>,
        firstQuery: Int,
        arguments: MutableMap<KParameter, Any?>
    ) { arguments.put(context, (remote as? InetSocketAddress)?.hostName ?: "") }
}