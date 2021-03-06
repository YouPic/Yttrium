package com.rimmer.metrics.server

import com.rimmer.metrics.generated.type.MetricPacket
import com.rimmer.metrics.server.generated.client.serverMetric
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary
import io.netty.channel.EventLoop
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/** Minimum time between connect attempts in milliseconds. */
val reconnectTimeout = 10000L

/** Sends received metrics to a metrics server. */
class ServerSender(
    context: ServerContext,
    val serverName: String,
    val host: String,
    val port: Int = 1338,
    val useNative: Boolean = false,
    sendInterval: Int = 60 * 1000
): (MetricPacket) -> Unit {
    // Run the sender on a single thread to prevent internal synchronization.
    private val eventLoop: EventLoop = context.handlerGroup.next()
    private val queue = ConcurrentLinkedQueue<MetricPacket>()

    private var client: BinaryClient? = null
    private var lastTry = 0L

    init {
        ensureClient()

        // Periodically send queued events to the server.
        eventLoop.scheduleAtFixedRate({
            ensureClient()
            client?.let {
                sendMetrics(it)
            }
            queue.clear()
        }, sendInterval.toLong(), sendInterval.toLong(), TimeUnit.MILLISECONDS)
    }

    override fun invoke(metric: MetricPacket) {
        queue.offer(metric)
    }

    private fun sendMetrics(client: BinaryClient) {
        val metrics = ArrayList<MetricPacket>()
        queue.forEach {
            metrics.add(it)
        }

        client.serverMetric(metrics, serverName) { _, e ->
            // If the sending failed we discard the events,
            // as queueing too many metrics could cause spikes in server latency.
            if(e != null) println("Could not send event batch: $e")
        }
    }

    private fun ensureClient() {
        val client = client
        if(client == null || !client.connected || client.responseTimer > 2*60*1000) {
            client?.close()
            this.client = null

            val time = System.currentTimeMillis()
            if(time - lastTry < reconnectTimeout) return
            lastTry = time

            connectBinary(eventLoop, host, port, useNative = useNative) { c, e ->
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