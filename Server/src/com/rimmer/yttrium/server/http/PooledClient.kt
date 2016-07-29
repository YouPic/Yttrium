package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.Context
import com.rimmer.yttrium.Task
import com.rimmer.yttrium.getOrAdd
import com.rimmer.yttrium.parseInt
import com.rimmer.yttrium.server.ServerContext
import io.netty.buffer.ByteBuf
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import java.util.*

class HttpPooledClient(context: ServerContext) {
    private val pools: Map<EventLoop, HashMap<String, SingleThreadPool>>

    init {
        // We initialize the pool at creation to avoid synchronization when lazily creating pools.
        val map = HashMap<EventLoop, HashMap<String, SingleThreadPool>>()
        context.handlerGroup.forEach { loop ->
            if(loop is EventLoop) {
                map[loop] = HashMap()
            }
        }

        pools = map
    }

    fun get(context: Context, url: String, headers: HttpHeaders? = null) =
        request(context, url, HttpMethod.GET, headers)

    fun post(
        context: Context,
        url: String,
        headers: HttpHeaders? = null,
        body: ByteBuf? = null,
        contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON
    ) = request(context, url, HttpMethod.POST, headers, body, contentType)

    fun put(
        context: Context,
        url: String,
        headers: HttpHeaders? = null,
        body: ByteBuf? = null,
        contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON
    ) = request(context, url, HttpMethod.PUT, headers, body, contentType)

    fun delete(
        context: Context,
        url: String,
        headers: HttpHeaders? = null,
        body: ByteBuf? = null,
        contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON
    ) = request(context, url, HttpMethod.DELETE, headers, body, contentType)

    fun request(
        context: Context,
        url: String,
        method: HttpMethod,
        headers: HttpHeaders? = null,
        body: ByteBuf? = null,
        contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON
    ): Task<FullHttpResponse> {
        val loop = context.eventLoop
        val pool = pools[loop] ?:
            throw IllegalArgumentException("Invalid context event loop - the event loop must be in the server's handlerGroup.")

        val split = url.split("://")
        val domain: String
        val path: String
        val isSsl: Boolean

        if(split.size > 1) {
            isSsl = split[0] == "https"
            domain = split[1].substringBefore('/')
            path = split[1].substring(domain.length)
        } else {
            isSsl = false
            domain = split[0].substringBefore('/')
            path = split[0].substring(domain.length)
        }

        val portString = domain.substringAfter(':', "")
        val port = if(portString.isNotEmpty()) parseInt(portString) else if(isSsl) 443 else 80

        val selector = "$domain $isSsl"


        val client = pool.getOrAdd(selector) { SingleThreadPool(PoolConfiguration(4)) {
            connectHttp(loop, domain, port, isSsl, 30000, it)
        } }

        val request = if(body === null) {
            DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
        } else {
            DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, body)
        }

        val httpHeaders = request.headers()
        httpHeaders[HttpHeaderNames.HOST] = domain

        if(body !== null) {
            httpHeaders[HttpHeaderNames.CONTENT_TYPE] = contentType
            if(headers !== null && !headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = body.writerIndex()
            }
        }

        if(headers !== null) {
            httpHeaders.add(headers)
        }

        val task = Task<FullHttpResponse>()
        client.get { c, e ->
            if(e == null) {
                c!!.request(request) { r, e ->
                    c.close()
                    if(e == null) {
                        task.finish(r!!)
                    } else {
                        task.fail(e)
                    }
                }
            } else {
                task.fail(e)
            }
        }
        return task
    }
}
