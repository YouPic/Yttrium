package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.HttpException
import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.router.HttpMethod
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpMethod as NettyMethod

fun httpDefault(c: ChannelHandlerContext, r: HttpRequest, f: (HttpResponse) -> Unit) {
    val status = if(r.method() === NettyMethod.OPTIONS) {
        HttpResponseStatus.OK
    } else {
        HttpResponseStatus.METHOD_NOT_ALLOWED
    }

    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
    f(response)
}

/** Parses a Netty http method. */
fun convertMethod(method: NettyMethod) = when(method.name()) {
    "GET" -> HttpMethod.GET
    "POST" -> HttpMethod.POST
    "DELETE" -> HttpMethod.DELETE
    "PUT" -> HttpMethod.PUT
    else -> null
}

/** Converts an exception type into an error response. */
fun mapError(error: Throwable?) = when(error) {
    is InvalidStateException -> errorResponse(HttpResponseStatus.BAD_REQUEST, error.message ?: "bad request")
    is UnauthorizedException -> errorResponse(HttpResponseStatus.FORBIDDEN, error.message ?: "forbidden")
    is NotFoundException -> errorResponse(HttpResponseStatus.NOT_FOUND, error.message ?: "not found")
    is HttpException -> errorResponse(HttpResponseStatus.valueOf(error.errorCode), error.message ?: "error")
    else -> errorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "internal error")
}

/** Creates an error response with the provided error code and text. */
fun errorResponse(error: HttpResponseStatus, text: String): HttpResponse {
    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, error, Unpooled.wrappedBuffer(text.toByteArray()))
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
    return response
}
