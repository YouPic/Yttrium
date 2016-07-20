package com.rimmer.metrics.server

import com.rimmer.metrics.server.generated.api.clientApi
import com.rimmer.metrics.server.generated.api.serverApi
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.finished
import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.RouteModifier
import com.rimmer.yttrium.router.RouteProperty
import com.rimmer.yttrium.router.Router
import com.rimmer.yttrium.router.plugin.Plugin
import com.rimmer.yttrium.serialize.stringReader
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryRouter
import com.rimmer.yttrium.server.binary.listenBinary

/** Runs a server that receives metrics and stores them in-memory. */
fun storeServer(context: ServerContext, store: MetricStore, port: Int) {
    val router = Router(emptyList())
    router.serverApi(
        statistic = {
            store.onStat(it)
            finish()
        },
        error = {
            store.onError(it)
            finish()
        },
        profile = {
            store.onProfile(it)
            finish()
        }
    )

    listenBinary(context, port, null, BinaryRouter(router))
}

/** Runs a server that listens for client requests and sends metrics data. */
fun clientServer(context: ServerContext, store: MetricStore, port: Int, password: String) {
    val router = Router(listOf(PasswordPlugin(password)) as List<Plugin<in Any>>)

    router.clientApi(
        getStats = {from: Long, to: Long ->
            finished(store.getStats(from, to))
        },
        getProfile = {from: Long, to: Long ->
            finished(store.getProfiles(from, to))
        },
        getError = {from: Long ->
            finished(store.getErrors(from))
        }
    )

    listenBinary(context, port, null, BinaryRouter(router))
}

class PasswordPlugin(val password: String): Plugin<Int> {
    override val name = "PasswordPlugin"

    override fun modifyRoute(modifier: RouteModifier, properties: List<RouteProperty>): Int {
        return modifier.addArg("password", stringReader)
    }

    override fun modifyCall(context: Int, route: RouteContext, arguments: Array<Any?>, f: (Throwable?) -> Unit) {
        if((arguments[context] as String) != password) f(UnauthorizedException())
        else f(null)
    }
}