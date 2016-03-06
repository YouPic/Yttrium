package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.serialize.readVarInt
import com.rimmer.yttrium.serialize.writeVarInt
import com.rimmer.yttrium.serialize.writeVarLong
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

open class BinaryDecoder: ChannelInboundHandlerAdapter() {
    private var accumulator: ByteBuf? = null

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if(message is ByteBuf) {
            // If we had leftover data in the previous chunk it is added to the beginning of the next packet.
            val packet = if(accumulator != null) {
                Unpooled.wrappedBuffer(accumulator, message)
            } else message

            // Decode every full packet inside this chunk.
            while(readPacket(packet) {r, b -> handlePacket(context, r, b) }) {}

            // If there is a partial packet left, we save it until more data is received.
            if(packet.readableBytes() > 0) {
                // This will copy a few bytes, but doing so is better than
                // creating a chain of wrapped buffers holding onto native memory.
                accumulator = Unpooled.buffer(packet.readableBytes())
                accumulator!!.writeBytes(packet)
            } else {
                accumulator = null
            }

            // Releasing this buffer will also release any wrapped buffers inside it.
            packet.release()
        }
    }

    override fun channelReadComplete(context: ChannelHandlerContext) {
        context.flush()
    }

    open fun handlePacket(context: ChannelHandlerContext, request: Int, packet: ByteBuf) {}
}

inline fun readPacket(source: ByteBuf, f: (Int, ByteBuf) -> Unit): Boolean {
    if(source.readableBytes() <= 4) return false
    source.markReaderIndex()

    // Each packet starts with a packet length and a request id.
    val length = source.readInt()
    val index = source.readerIndex()
    val request = source.readVarInt()

    // If we don't have the full buffer yet we wait for more data.
    if(source.readableBytes() + (source.readerIndex() - index) < length) {
        source.resetReaderIndex()
        return false
    } else {
        f(request, source)
        return true
    }
}

inline fun writePacket(context: ChannelHandlerContext, request: Int, writer: (ByteBuf, () -> Unit) -> Unit) {
    // Create a response buffer with space for the length.
    val target = context.alloc().buffer()
    target.writeInt(0)
    target.writeVarInt(request)

    // Send the request to the handler.
    writer(target) {
        // Set the final packet size.
        target.setInt(0, target.writerIndex() - 4)
        context.writeAndFlush(target, context.voidPromise())
    }
}

fun writeNullMap(values: Array<Any?>, target: ByteBuf) {
    var map = 0L
    values.forEachIndexed { i, v ->
        if(v != null) {
            map = map or (1L shl i)
        }
    }
    target.writeVarLong(map)
}