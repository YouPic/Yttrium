package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.listen
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil

fun listenHttp(
    context: ServerContext,
    port: Int,
    handler: (FullHttpRequest, (HttpResponse) -> Unit) -> Unit
) = listen(context, port) {
    addLast(HttpResponseEncoder(), HttpRequestDecoder(), HttpObjectAggregator(10 * 1024 * 1024), HttpHandler(handler))
}

class HttpHandler(val f: (FullHttpRequest, (HttpResponse) -> Unit) -> Unit): ChannelInboundHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        try {
            if(message is FullHttpRequest) f(message) {
                if(HttpUtil.isKeepAlive(message)) {
                    val contentLength = (it as? FullHttpResponse)?.content()?.readableBytes() ?: 0
                    it.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength)
                    it.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    context.writeAndFlush(it)
                } else {
                    context.writeAndFlush(it).addListener(ChannelFutureListener.CLOSE)
                }
            }
        } finally {
            ReferenceCountUtil.release(message)
        }
    }

    override fun channelReadComplete(context: ChannelHandlerContext) {
        context.flush()
    }
}
