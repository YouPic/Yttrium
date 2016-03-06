package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.serialize.readVarInt
import com.rimmer.yttrium.serialize.writeVarInt
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.listen
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

fun listenBinary(
    context: ServerContext,
    port: Int,
    handler: (ChannelHandlerContext, source: ByteBuf, target: ByteBuf, push: () -> Unit) -> Unit
) = listen(context, port) {
    addLast(BinaryHandler(handler))
}

class BinaryHandler(
    val f: (ChannelHandlerContext, source: ByteBuf, target: ByteBuf, push: () -> Unit) -> Unit
): ChannelInboundHandlerAdapter() {
    private var accumulator: ByteBuf? = null

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if(message is ByteBuf) {
            // If we had leftover data in the previous chunk it is added to the beginning of the next packet.
            val packet = if(accumulator == null) {
                Unpooled.wrappedBuffer(accumulator, message)
            } else message

            // Decode every full packet inside this chunk.
            while(readPacket(context, packet)) {}

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

    private fun readPacket(context: ChannelHandlerContext, source: ByteBuf): Boolean {
        if(source.readableBytes() <= 2) return false
        source.markReaderIndex()

        // Each packet starts with a packet length and a request id.
        val length = source.readVarInt()
        val request = source.readVarInt()

        // If we don't have the full buffer yet we wait for more data.
        if(source.readableBytes() < length) {
            source.resetReaderIndex()
            return false
        }

        // Create a response buffer with space for the length.
        val target = context.alloc().buffer()
        target.writeVarInt(0)
        target.writeVarInt(request)

        // Send the request to the handler.
        f(context, source, target) {
            context.writeAndFlush(target)
        }

        return true
    }
}
