package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.listen
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import io.netty.util.ReferenceCountUtil

fun listenHttp(
    context: ServerContext,
    port: Int,
    cors: Boolean = false,
    pipeline: (ChannelPipeline.() -> Unit)? = null,
    handler: (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit
) = listen(context, port) {
    addLast(HttpResponseEncoder(), HttpRequestDecoder())
    addLast(HttpObjectAggregator(10 * 1024 * 1024), HttpHandler(handler, cors))
    pipeline?.invoke(this)
}

class HttpHandler(
    val f: (ChannelHandlerContext, FullHttpRequest, (HttpResponse) -> Unit) -> Unit,
    val cors: Boolean
): ChannelInboundHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        try {
            if(message is FullHttpRequest) {
                if(cors && message.method() === HttpMethod.OPTIONS) {
                    sendResponse(context, DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.EMPTY_BUFFER
                    ), 0)
                    return
                }

                f(context, message) {
                    val contentLength = (it as? FullHttpResponse)?.content()?.readableBytes() ?: 0
                    sendResponse(context, it, contentLength)
                }
            }
        } finally {
            ReferenceCountUtil.release(message)
        }
    }

    override fun channelReadComplete(context: ChannelHandlerContext) {
        context.flush()
    }

    private fun handleCors(headers: HttpHeaders) {
        if(cors) {
            headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders)
            headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods)
            headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin)
            headers.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, allowedAge)
        }
    }

    private fun sendResponse(context: ChannelHandlerContext, response: HttpResponse, length: Int) {
        val headers = response.headers()
        handleCors(headers)

        if(HttpUtil.isKeepAlive(response)) {
            headers.set(HttpHeaderNames.CONTENT_LENGTH, length)
            headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            context.writeAndFlush(response, context.voidPromise())
        } else {
            context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        }
    }

    companion object {
        val allowedHeaders = AsciiString.of("Accept,API-VERSION,Content-Type")
        val allowedMethods = AsciiString.of("DELETE,POST,GET,PUT")
        val allowedOrigin = AsciiString.of("*")
        val allowedAge = AsciiString.of("3628800")
    }
}
