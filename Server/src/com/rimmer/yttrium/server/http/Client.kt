package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.serialize.writeJson
import com.rimmer.yttrium.server.ServerContext
import com.rimmer.yttrium.server.connect
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder
import io.netty.handler.codec.http.multipart.MemoryAttribute
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import javax.net.ssl.SSLContext

interface HttpClient {
    fun request(request: HttpRequest, f: (FullHttpResponse?, Throwable?) -> Unit)
    fun request(method: HttpMethod, path: String, f: (FullHttpResponse?, Throwable?) -> Unit)
    fun request(method: HttpMethod, path: String, body: Any, f: (FullHttpResponse?, Throwable?) -> Unit)
    fun request(method: HttpMethod, path: String, body: Array<Pair<String, Any>>, f: (FullHttpResponse?, Throwable?) -> Unit)

    /** Closes this connection. Any calls after this will fail. */
    fun close()

    /** Set to true as long as the connection is active. */
    val connected: Boolean

    /** Set to true if the connection is currently waiting for a response. */
    val busy: Boolean

    /** The nanoTime timestamp where the last request on this connection was handled. */
    val lastRequest: Long
}

fun connectHttp(context: ServerContext, host: String, port: Int, ssl: Boolean = false, timeout: Int = 0, f: (HttpClient?, Throwable?) -> Unit) {
    connect(context, host, port, timeout, {
        val sslContext = if(ssl) {
            val sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            val handler = SslHandler(sslContext.newEngine(ByteBufAllocator.DEFAULT, host, port))
            addLast(handler)
            handler
        } else null

        addLast(
            HttpResponseDecoder(),
            HttpRequestEncoder(),
            HttpObjectAggregator(16 * 1024 * 1024),
            HttpContentDecompressor(),
            HttpClientHandler(f, sslContext)
        )
    }, {
        f(null, it)
    })
}

class HttpClientHandler(val onConnect: (HttpClient?, Throwable?) -> Unit, val ssl: SslHandler?): ChannelInboundHandlerAdapter(), HttpClient {
    private var context: ChannelHandlerContext? = null
    private var listener: ((FullHttpResponse?, Throwable?) -> Unit)? = null
    private var lastFinish = System.nanoTime()

    override val connected: Boolean get() = context != null && context!!.channel().isActive
    override val busy: Boolean get() = listener != null

    override val lastRequest: Long get() = lastFinish

    override fun channelActive(context: ChannelHandlerContext) {
        this.context = context
        if(ssl == null) {
            onConnect(this, null)
        } else {
            ssl.handshakeFuture().addListener {
                if(it.isSuccess) {
                    onConnect(this, null)
                } else {
                    onConnect(null, it.cause())
                }
            }
        }
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        // Fail any pending request.
        val error = IOException("Connection was closed.")
        listener?.invoke(null, error)
        listener = null
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if(message is FullHttpResponse) {
            lastFinish = System.nanoTime()
            listener?.invoke(message, null)
            listener = null
        }
    }

    override fun request(request: HttpRequest, f: (FullHttpResponse?, Throwable?) -> Unit) {
        listener = f
        context!!.writeAndFlush(request, context!!.voidPromise())
    }

    override fun request(method: HttpMethod, path: String, f: (FullHttpResponse?, Throwable?) -> Unit) {
        listener = f
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
        context!!.writeAndFlush(request, context!!.voidPromise())
    }

    override fun request(method: HttpMethod, path: String, body: Any, f: (FullHttpResponse?, Throwable?) -> Unit) {
        listener = f
        val buffer = ByteBufAllocator.DEFAULT.buffer()
        writeJson(body, buffer)
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, buffer)
        context!!.writeAndFlush(request, context!!.voidPromise())
    }

    override fun request(method: HttpMethod, path: String, body: Array<Pair<String, Any>>, f: (FullHttpResponse?, Throwable?) -> Unit) {
        listener = f
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
        val encoder = HttpPostRequestEncoder(request, false)
        for((k, v) in body) {
            val buffer = ByteBufAllocator.DEFAULT.buffer()
            writeJson(v, buffer)
            val attribute = MemoryAttribute(k)
            attribute.setContent(buffer)
            encoder.addBodyHttpData(attribute)
        }

        context!!.writeAndFlush(encoder.finalizeRequest(), context!!.voidPromise())
    }

    override fun close() {
        if(ssl == null) {
            context?.close()
        } else {
            ssl.close().addListener {context?.close()}
        }
    }
}