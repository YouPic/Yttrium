package com.rimmer.metrics.server

import com.rimmer.metrics.Sender
import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.generated.type.ProfilePacket
import com.rimmer.metrics.generated.type.StatPacket
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary
import com.rimmer.yttrium.server.binary.routeHash
import org.joda.time.DateTime

/** Minimum time between connect attempts in milliseconds. */
val reconnectTimeout = 10000L

/** Sends received metrics to a metrics server. */
class ServerSender(val context: ServerContext, val host: String, val port: Int = 1338): Sender {
    val statisticRoute = routeHash("POST /statistic")
    val profileRoute = routeHash("POST /profile")
    val errorRoute = routeHash("POST /error")

    var client: BinaryClient? = null
    var lastTry = 0L

    init {
        ensureClient()
    }

    override fun sendStatistic(stat: StatPacket) {
        ensureClient()
        client?.call(statisticRoute, emptyArray(), arrayOf(stat), Unit::class.java)
    }

    override fun sendProfile(profile: ProfilePacket) {
        ensureClient()
        client?.call(profileRoute, emptyArray(), arrayOf(profile), Unit::class.java)
    }

    override fun sendError(error: ErrorPacket) {
        ensureClient()
        client?.call(errorRoute, emptyArray(), arrayOf(error), Unit::class.java)
    }

    private fun ensureClient() {
        val client = client
        if(client == null || !client.connected) {
            this.client = null

            val time = System.nanoTime()
            if(time - lastTry < reconnectTimeout * 1000000) return
            lastTry = time

            connectBinary(context, host, port) { c, e ->
                if(e == null) {
                    println("Connected to metrics server.")
                } else {
                    println("Failed to connect to metrics server: $e. Retrying in at least $reconnectTimeout ms.")
                }
                this.client = c
            }
        }
    }
}