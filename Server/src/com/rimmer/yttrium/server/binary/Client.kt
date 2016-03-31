package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.connect
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import java.io.IOException
import java.util.*

interface BinaryClient {
    /**
     * Calls a route and receives its result.
     * @param route The hash of the route to call
     * @param path An ordered set of path parameters.
     * @param queries An ordered set of query parameters.
     * @param target The expected result type of the route.
     * @param f Called to receive the result.
     */
    fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit)

    /**
     * Calls a route without receiving its result.
     * @param route The hash of the route to call
     * @param path An ordered set of path parameters.
     * @param queries An ordered set of query parameters.
     * @param target The expected result type of the route (this is needed by the protocol).
     */
    fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>)

    /**
     * Calls a route and subscribes on its results.
     * @param route The hash of the route to call
     * @param path An ordered set of path parameters.
     * @param queries An ordered set of query parameters.
     * @param target The expected result type of the route.
     * @param f Called to receive the results. This will continue being called until the subscription is closed.
     * @return A subscription id that can be used to unsubscribe.
     */
    fun subscribe(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit): Int

    /**
     * Stops receiving messages from the provided subscription id.
     */
    fun unsubscribe(subscription: Int)

    /** Closes this connection. Any calls after this will fail. */
    fun close()

    /** Set to true as long as the connection is active. */
    val connected: Boolean
}

fun connectBinary(
    context: ServerContext,
    host: String,
    port: Int,
    timeout: Int = 0,
    onConnect: (BinaryClient?, Throwable?) -> Unit
) = connect(context, host, port, timeout, {
    addLast(BinaryClientHandler(onConnect))
}, {
    onConnect(null, it)
})

class BinaryClientHandler(val onConnect: (BinaryClient?, Throwable?) -> Unit): BinaryDecoder(), BinaryClient {
    private data class Request(val target: Class<*>, val handler: ((Any?, Throwable?) -> Unit)?, val isPush: Boolean)

    private var context: ChannelHandlerContext? = null
    private val requests = ArrayList<Request?>()
    private var nextRequest = 0

    override val connected: Boolean get() = context != null && context!!.channel().isActive

    override fun channelActive(context: ChannelHandlerContext) {
        this.context = context
        onConnect(this, null)
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        // Fail all pending requests.
        val error = IOException("Connection was closed.")
        for(r in requests) {
            r?.handler?.invoke(null, error)
        }
        requests.clear()
    }

    override fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>) {
        performRequest(Request(target, null, false), route, path, queries)
    }

    override fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit) {
        performRequest(Request(target, f, false), route, path, queries)
    }

    override fun subscribe(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit): Int {
        return performRequest(Request(target, null, true), route, path, queries)
    }

    override fun unsubscribe(subscription: Int) {
        finishRequest(subscription)
    }

    override fun close() {
        context?.close()
    }

    override fun handlePacket(context: ChannelHandlerContext, request: Int, packet: ByteBuf) {
        if(requests.size <= request || requests[request] == null) {
            // TODO: Send this to a listener.
            println("Error in BinaryClientHandler: Unknown request id")
            return
        }

        val requestData = requests[request]!!
        mapResponse(requestData, packet)

        // Only remove the request if it was one-use.
        if(!requestData.isPush) {
            finishRequest(request)
        }
    }

    private fun performRequest(r: Request, route: Int, path: Array<Any?>, queries: Array<Any?>): Int {
        val id = addRequest(r)
        writePacket(context!!, id) { target, commit ->
            target.writeVarInt(route)

            for(p in path) {
                writeBinary(p, target)
            }

            writeNullMap(queries, target)
            for(q in queries) {
                writeBinary(q, target)
            }
            commit()
        }
        return id
    }

    private fun addRequest(r: Request): Int {
        val i = nextRequest
        if(i >= requests.size) {
            requests.add(r)
            nextRequest++
            return i
        } else {
            requests[i] = r
            val next = requests.indexOfFirst { it == null }
            nextRequest = if(next == -1) requests.size else next
            return i
        }
    }

    private fun finishRequest(request: Int) {
        requests[request] = null
        nextRequest = request
    }

    private fun mapResponse(request: Request, packet: ByteBuf) {
        val response = packet.readByte().toInt()
        if(response == ResponseCode.Success.ordinal) {
            val result = readBinary(packet, request.target)
            request.handler?.invoke(result, null)
        } else {
            val error = packet.readString()
            val exception = when(response) {
                ResponseCode.InvalidArgs.ordinal, ResponseCode.NoRoute.ordinal -> InvalidStateException(error)
                ResponseCode.NoPermission.ordinal -> UnauthorizedException()
                ResponseCode.NotFound.ordinal -> NotFoundException()
                else -> Exception(error)
            }
            request.handler?.invoke(null, exception)
        }
    }
}
