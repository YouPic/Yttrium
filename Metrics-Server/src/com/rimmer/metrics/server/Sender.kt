package com.rimmer.metrics.server

import com.rimmer.metrics.ErrorPacket
import com.rimmer.metrics.ProfilePacket
import com.rimmer.metrics.Sender
import com.rimmer.metrics.StatPacket
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary
import com.rimmer.yttrium.server.binary.routeHash

/** Sends received metrics to a metrics server. */
class ServerSender(val context: ServerContext, val host: String, val port: Int = 1338): Sender {
    val statisticRoute = routeHash("POST /statistic")
    val profileRoute = routeHash("POST /profile")
    val errorRoute = routeHash("POST /error")

    var client: BinaryClient? = null

    init {
        connectBinary(context, host, port) { c, e -> client = c }
    }

    override fun sendStatistic(stat: StatPacket) {
        client?.call(statisticRoute, emptyArray(), arrayOf(stat), Unit::class.java)
    }

    override fun sendProfile(profile: ProfilePacket) {
        client?.call(profileRoute, emptyArray(), arrayOf(profile), Unit::class.java)
    }

    override fun sendError(error: ErrorPacket) {
        client?.call(errorRoute, emptyArray(), arrayOf(error), Unit::class.java)
    }
}