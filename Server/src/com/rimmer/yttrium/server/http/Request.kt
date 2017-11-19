package com.rimmer.yttrium.server.http

import com.rimmer.yttrium.*
import com.rimmer.yttrium.router.HttpMethod
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.serialize.JsonWriter
import com.rimmer.yttrium.serialize.writeJson
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
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
fun mapError(error: Throwable?, route: Route, headers: HttpHeaders): HttpResponse {
    val code: HttpResponseStatus
    val message: String

    when(error) {
        is InvalidStateException -> {
            code = HttpResponseStatus.BAD_REQUEST
            message = error.message ?: "bad request"
        }
        is UnauthorizedException -> {
            code = HttpResponseStatus.FORBIDDEN
            message = error.message ?: "forbidden"
        }
        is NotFoundException -> {
            code = HttpResponseStatus.NOT_FOUND
            message = error.message ?: "not found"
        }
        is HttpException -> {
            code = HttpResponseStatus.valueOf(error.errorCode)
            message = error.message ?: "error"
        }
        else -> {
            code = HttpResponseStatus.INTERNAL_SERVER_ERROR
            message = "internal error"
        }
    }

    val buffer = try {
        val response = (error as? RouteException)?.response
        if(response is ByteBuf) {
            response
        } else {
            val buffer = ByteBufAllocator.DEFAULT.buffer()
            val json = JsonWriter(buffer)

            if(response == null) {
                json.startObject().field("error").value(message).endObject()
            } else {
                writeJson(response, route.writer, buffer)
            }
            buffer
        }
    } catch(e: Exception) {
        null
    }

    if(!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
        headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    }

    return DefaultFullHttpResponse(HttpVersion.HTTP_1_1, code, buffer, headers, DefaultHttpHeaders(false))
}
