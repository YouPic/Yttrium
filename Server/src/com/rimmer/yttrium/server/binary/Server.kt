package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.listen
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

fun listenBinary(
    context: ServerContext,
    port: Int,
    handler: (ChannelHandlerContext, source: ByteBuf, target: ByteBuf, push: () -> Unit) -> Unit
) = listen(context, port) {
    addLast(BinaryHandler(handler))
}

class BinaryHandler(
    val f: (ChannelHandlerContext, source: ByteBuf, target: ByteBuf, push: () -> Unit) -> Unit
): BinaryDecoder() {
    override fun handlePacket(context: ChannelHandlerContext, request: Int, packet: ByteBuf) {
        writePacket(context, request) { target, commit ->
            f(context, packet, target, commit)
        }
    }
}
