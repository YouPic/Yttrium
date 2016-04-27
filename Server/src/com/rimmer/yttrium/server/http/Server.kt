package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.listen
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.cors.CorsConfig
import io.netty.handler.codec.http.cors.CorsConfigBuilder
import io.netty.handler.codec.http.cors.CorsHandler
import io.netty.util.ReferenceCountUtil

fun listenHttp(
    context: ServerContext,
    port: Int,
    cors: Boolean = false,
    pipeline: (ChannelPipeline.() -> Unit)? = null,
    handler: (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit
) = listen(context, port) {
    addLast(HttpResponseEncoder(), HttpRequestDecoder())

    if(cors) {
        addLast(CorsHandler(CorsConfigBuilder.forAnyOrigin()
            .allowedRequestHeaders("Accept", "Content-Type", "API-VERSION")
            .allowedRequestMethods(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE)
            .build()))
    }

    addLast(HttpObjectAggregator(10 * 1024 * 1024), HttpHandler(handler))
    pipeline?.invoke(this)
}

class HttpHandler(val f: (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit): ChannelInboundHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        try {
            if(message is FullHttpRequest) f(context, message) {
                if(HttpUtil.isKeepAlive(message)) {
                    val contentLength = (it as? FullHttpResponse)?.content()?.readableBytes() ?: 0
                    it.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength)
                    it.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    context.writeAndFlush(it, context.voidPromise())
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
