package com.rimmer.metrics.server

import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.generated.type.ProfilePacket
import com.rimmer.metrics.generated.type.StatPacket
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.finished
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

    // Force the packet types to be initialized.
    StatPacket.Companion
    ErrorPacket.Companion
    ProfilePacket.Companion

    router.post("statistic").arg("stat").handle {it: StatPacket ->
        store.onStat(it)
        finish()
    }

    router.post("error").arg("error").handle {it: ErrorPacket ->
        store.onError(it)
        finish()
    }

    router.post("profile").arg("profile").handle {it: ProfilePacket ->
        store.onProfile(it)
        finish()
    }

    listenBinary(context, port, null, BinaryRouter(router))
}

/** Runs a server that listens for client requests and sends metrics data. */
fun clientServer(context: ServerContext, store: MetricStore, port: Int, password: String) {
    val router = Router(emptyList())

    router.get("stats/:from/:to").arg("password").handle {from: Long, to: Long, pw: String ->
        if(pw != password) throw UnauthorizedException()
        finished(store.getStats(from, to))
    }

    listenBinary(context, port, null, BinaryRouter(router))
}