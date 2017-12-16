package com.rimmer.yttrium.server.binary

import com.rimmer.metrics.Metrics
import com.rimmer.yttrium.Context
import com.rimmer.yttrium.Task
import com.rimmer.yttrium.logMessage
import com.rimmer.yttrium.logWarning
import com.rimmer.yttrium.server.ServerContext
import java.io.IOException
import java.util.*

/**
 * Represents a persistent connection to a client with a binary api.
 * Automatically handles reconnects and queuing requests while doing so.
 * Note that reconnecting is currently based on new requests coming in -
 * adding a single request without anything more will not handle reconnecting.
 */
class BinaryConnection(
    val context: ServerContext,
    val host: String,
    val port: Int,
    val connectTimeout: Int = 60 * 1000,
    val requestTimeout: Int = 120 * 1000,
    val reconnectInterval: Int = 4 * 1000,
    val maxRetries: Int = 2,
    val name: String = "server",
    val useNative: Boolean = true,
    val metrics: Metrics? = null
) {
    private val metricName = "Connection:$name"

    inner class Command<T>(val context: Context, val added: Long, private val f: (BinaryClient) -> Task<T>, val task: Task<T>, val metricCall: Any?, val metricId: Int) {
        private var retries: Int = 0

        operator fun invoke(client: BinaryClient) {
            f(client).handler = { r, e ->
                if(e == null) {
                    metrics?.endEvent(metricCall, metricId)
                    task.finish(r!!)
                } else if(e is IOException && retries < maxRetries) {
                    logWarning("${name.capitalize()} request failed due to network, trying again...")
                    retries++
                    add(this)
                } else {
                    metrics?.endEvent(metricCall, metricId)
                    task.fail(e)
                }
            }
        }
    }

    fun <T> add(context: Context, f: (BinaryClient) -> Task<T>): Task<T> {
        val metric = metrics?.startEvent(context.listenerData, metricName, "request")
        val task = Task<T>()
        add(Command(context, System.currentTimeMillis(), f, task, context.listenerData, metric ?: 0))
        return task
    }

    private fun <T> add(command: Command<T>) {
        val client = ensureClient(command.context)
        val handler = client.handler

        if(handler === null) {
            client.queue.offer(command)
        } else {
            command(handler)
        }
    }

    private class Client {
        var handler: BinaryClient? = null
        var lastConnect = 0L
        val queue = ArrayDeque<Command<*>>()
    }

    private val clients = context.handlerGroup.associate { it to Client() }

    private fun ensureClient(context: Context): Client {
        val client = clients[context.eventLoop] ?:
            throw IllegalArgumentException("Invalid context event loop - the event loop must be in the server's handlerGroup.")

        val handler = client.handler
        if(handler === null || !handler.connected || (handler.pendingRequests > 0 && handler.responseTimer > connectTimeout)) {
            handler?.close()
            client.handler = null

            val time = System.currentTimeMillis()
            if(time - client.lastConnect < reconnectInterval) return client
            client.lastConnect = time

            val i = client.queue.iterator()
            while(i.hasNext()) {
                val it = i.next()
                if(time - it.added > requestTimeout) {
                    it.task.fail(IOException("Request timed out."))
                    i.remove()
                }
            }

            connectBinary(context.eventLoop, host, port, useNative = useNative) { c, e ->
                client.handler = c
                if(e == null) {
                    logMessage("Connected to $name.")
                    val queue = client.queue
                    while(queue.isNotEmpty()) {
                        queue.poll()(c!!)
                    }
                } else {
                    logWarning("Failed to connect to $name: $e. Retrying in at least $reconnectInterval ms.")
                }
            }
        }

        return client
    }
}