package com.rimmer.metrics.server

import com.rimmer.metrics.Sender
import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.generated.type.ProfilePacket
import com.rimmer.metrics.generated.type.StatPacket
import com.rimmer.metrics.server.generated.client.*
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary

/** Minimum time between connect attempts in milliseconds. */
val reconnectTimeout = 10000L

/** Sends received metrics to a metrics server. */
class ServerSender(val context: ServerContext, val host: String, val port: Int = 1338): Sender {
    var client: BinaryClient? = null
    var lastTry = 0L

    init {
        ensureClient()
    }

    override fun sendStatistic(stat: StatPacket) {
        ensureClient()
        client?.serverStatistic(stat) { r, e -> }
    }

    override fun sendProfile(profile: ProfilePacket) {
        ensureClient()
        client?.serverProfile(profile) { r, e -> }
    }

    override fun sendError(error: ErrorPacket) {
        ensureClient()
        client?.serverError(error) { r, e -> }
    }

    private fun ensureClient() {
        val client = client
        if(client == null || !client.connected) {
            this.client = null

            val time = System.nanoTime()
            if(time - lastTry < reconnectTimeout * 1000000) return
            lastTry = time

            connectBinary(context.handlerGroup, host, port) { c, e ->
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