package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.serialize.writeJson
import com.rimmer.yttrium.server.connect
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.multipart.DiskAttribute
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder
import io.netty.handler.codec.http.multipart.MemoryAttribute
import io.netty.handler.codec.http.multipart.MixedAttribute
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class HttpResult(val status: HttpResponseStatus, val statusCode: Int, val headers: HttpHeaders)

/** Handles the result of an HTTP request. */
interface HttpListener {
    /**
     * Receives the initial response headers.
     * This is called once, before onContent.
     */
    fun onResult(result: HttpResult) {}

    /**
     * Receives response data. This is called at least once.
     * This can be called multiple times with chunks of the response.
     * @param finished If set, this is the last chunk and request has finished.
     */
    fun onContent(result: HttpResult, content: ByteBuf, finished: Boolean) {}

    /**
     * Called when an error occurs.
     * This can happen even after onResponse or onContent has been called,
     * but not once the request has finished.
     */
    fun onError(error: Throwable) {}
}

interface HttpClient {
    fun request(request: HttpRequest, body: Any?, listener: HttpListener)
    fun request(method: HttpMethod, path: String, listener: HttpListener)
    fun request(method: HttpMethod, path: String, body: Any, listener: HttpListener)
    fun request(method: HttpMethod, path: String, body: Array<Pair<String, Any>>, listener: HttpListener)

    /** Closes this connection. Any calls after this will fail. */
    fun close()

    /** Set to true as long as the connection is active. */
    val connected: Boolean

    /** Set to true if the connection is currently waiting for a response. */
    val busy: Boolean

    /** The nanoTime timestamp where the last request on this connection was handled. */
    val lastRequest: Long
}

fun connectHttp(loop: EventLoopGroup, host: String, port: Int, ssl: Boolean = false, timeout: Int = 0, f: (HttpClient?, Throwable?) -> Unit) {
    connect(loop, host, port, timeout, {
        val sslContext = if(ssl) {
            val sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            val handler = SslHandler(sslContext.newEngine(ByteBufAllocator.DEFAULT, host, port))
            addLast(handler)
            handler
        } else null

        addLast(
            HttpResponseDecoder(),
            HttpRequestEncoder(),
            HttpContentDecompressor(),
            HttpClientHandler(f, sslContext)
        )
    }, {
        f(null, it)
    })
}

class HttpClientHandler(val onConnect: (HttpClient?, Throwable?) -> Unit, val ssl: SslHandler?): ChannelInboundHandlerAdapter(), HttpClient {
    private var context: ChannelHandlerContext? = null
    private var listener: HttpListener? = null
    private var result: HttpResult? = null
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
        listener?.onError(error)
        listener = null
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        val listener = listener
        if(message is HttpResponse) {
            val result = HttpResult(message.status(), message.status().code(), message.headers())
            this.result = result
            listener?.onResult(result)
        } else if(message is HttpContent) {
            val result = result!!
            val finished = if(message is LastHttpContent) {
                lastFinish = System.nanoTime()
                this.listener = null
                this.result = null
                true
            } else false

            listener?.onContent(result, message.content(), finished)
        }
    }

    override fun request(request: HttpRequest, body: Any?, listener: HttpListener) {
        this.listener = listener
        val content = if(body is ByteBuf) DefaultLastHttpContent(body) else body

        if(body === null) {
            context!!.writeAndFlush(request, context!!.voidPromise())
        } else if(content is LastHttpContent) {
            context!!.write(request, context!!.voidPromise())
            context!!.writeAndFlush(content, context!!.voidPromise())
        } else {
            context!!.write(request, context!!.voidPromise())
            context!!.write(content, context!!.voidPromise())
            context!!.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, context!!.voidPromise())
        }
    }

    override fun request(method: HttpMethod, path: String, listener: HttpListener) {
        this.listener = listener
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
        context!!.writeAndFlush(request, context!!.voidPromise())
    }

    override fun request(method: HttpMethod, path: String, body: Any, listener: HttpListener) {
        this.listener = listener
        val buffer = ByteBufAllocator.DEFAULT.buffer()
        writeJson(body, null, buffer)
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, buffer)
        context!!.writeAndFlush(request, context!!.voidPromise())
    }

    override fun request(method: HttpMethod, path: String, body: Array<Pair<String, Any>>, listener: HttpListener) {
        this.listener = listener
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
        val encoder = HttpPostRequestEncoder(request, false)
        for((k, v) in body) {
            val attribute = when(v) {
                is File -> {
                    val attribute = DiskAttribute(k)
                    attribute.setContent(v)
                    attribute
                }
                is InputStream -> {
                    val attribute = MemoryAttribute(k)
                    attribute.setContent(v)
                    attribute
                }
                is ByteBuf -> {
                    val attribute = MemoryAttribute(k)
                    attribute.setContent(v)
                    attribute
                }
                else -> {
                    val buffer = ByteBufAllocator.DEFAULT.buffer()
                    writeJson(v, null, buffer)
                    val attribute = MemoryAttribute(k)
                    attribute.setContent(buffer)
                    attribute
                }
            }
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