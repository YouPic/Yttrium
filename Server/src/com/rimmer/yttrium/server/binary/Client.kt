package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.serialize.readBinary
import com.rimmer.yttrium.serialize.readString
import com.rimmer.yttrium.serialize.writeBinary
import com.rimmer.yttrium.serialize.writeVarInt
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.connect
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import java.util.*

interface BinaryClient {
    fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit)
    fun close()
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
    private data class Request(val target: Class<*>, val handler: (Any?, Throwable?) -> Unit)

    private var context: ChannelHandlerContext? = null
    private val requests = ArrayList<Request?>()
    private var nextRequest = 0

    override fun channelActive(context: ChannelHandlerContext) {
        this.context = context
        onConnect(this, null)
    }

    override fun call(route: Int, path: Array<Any?>, queries: Array<Any?>, target: Class<*>, f: (Any?, Throwable?) -> Unit) {
        writePacket(context!!, addRequest(Request(target, f))) { target, commit ->
            target.writeVarInt(route)
            for(p in path) {
                writeBinary(p, target)
            }
            for(q in queries) {
                writeBinary(q, target)
            }
            commit()
        }
    }

    override fun close() {
        context!!.close()
    }

    override fun handlePacket(context: ChannelHandlerContext, request: Int, packet: ByteBuf) {
        if(requests.size <= request || requests[request] == null) {
            // TODO: Send this to a listener.
            println("Error in BinaryClientHandler: Unknown request id")
            return
        }

        mapResponse(requests[request]!!, packet)
        finishRequest(request)
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
            request.handler(result, null)
        } else {
            val error = packet.readString()
            val exception = when(response) {
                ResponseCode.InvalidArgs.ordinal, ResponseCode.NoRoute.ordinal -> InvalidStateException(error)
                ResponseCode.NoPermission.ordinal -> UnauthorizedException()
                ResponseCode.NotFound.ordinal -> NotFoundException()
                else -> Exception(error)
            }
            request.handler(null, exception)
        }
    }
}
