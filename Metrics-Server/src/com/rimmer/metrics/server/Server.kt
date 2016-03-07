package com.rimmer.metrics.server

import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.generated.type.ProfilePacket
import com.rimmer.metrics.generated.type.StatPacket
import com.rimmer.yttrium.parseInt
import com.rimmer.yttrium.router.Router
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryRouter
import com.rimmer.yttrium.server.binary.listenBinary
import com.rimmer.yttrium.server.http.HttpRouter
import com.rimmer.yttrium.server.http.listenHttp
import com.rimmer.yttrium.server.runServer


/** Runs a server that receives metrics and stores them in-memory. */
fun storeServer(context: ServerContext, store: MetricStore, port: Int) {
    val router = Router(emptyList())

    router.post("statistic").arg("stat").handle {it: StatPacket ->
        store.onStat(it)
        succeed()
    }

    router.post("error").arg("error").handle {it: ErrorPacket ->
        store.onError(it)
        succeed()
    }

    router.post("profile").arg("profile").handle {it: ProfilePacket ->
        store.onProfile(it)
        succeed()
    }

    listenBinary(context, port, BinaryRouter(router))
}
