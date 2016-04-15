package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.RouteListener
import com.rimmer.yttrium.router.Router
import com.rimmer.yttrium.serialize.*
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import java.net.InetSocketAddress

enum class ResponseCode {
    Success, NoRoute, NotFound, InvalidArgs, NoPermission, InternalError
}

fun routeHash(r: Route) = routeHash(r.name, r.version)
fun routeHash(name: String, version: Int = 0) = name.hashCode() + version * 3452056

class BinaryRouter(
    val router: Router,
    val listener: RouteListener? = null
): (ChannelHandlerContext, ByteBuf, ByteBuf, () -> Unit) -> Unit {
    private val segmentMap = router.routes.associateBy { routeHash(it) }

    override fun invoke(context: ChannelHandlerContext, source: ByteBuf, target: ByteBuf, f: () -> Unit) {
        val remote = (context.channel().remoteAddress() as? InetSocketAddress)?.hostName ?: ""
        val id = source.readVarInt()
        val route = segmentMap[id]
        if(route == null) {
            error(ResponseCode.NoRoute, "Route $id not found", target, f)
            return
        }

        val eventLoop = context.channel().eventLoop()
        val callId = listener?.onStart(eventLoop, route) ?: 0
        val params = arrayOfNulls<Any>(route.typedSegments.size)
        val queries = arrayOfNulls<Any>(route.queries.size)

        // We can't really detect specific problems in the call parameters
        // without making everything really complicated,
        // so if the decoding fails we just return a simple error.
        try {
            route.typedSegments.forEachIndexed { i, segment ->
                params[i] = readBinary(source, segment.type!!)
            }

            val nullMap = source.readVarLong()
            route.queries.forEachIndexed { i, query ->
                if ((nullMap and (1L shl i)) != 0L) {
                    queries[i] = readBinary(source, query.type)
                } else if (query.optional) {
                    queries[i] = query.default
                } else {
                    val description = if (query.description.isNotEmpty()) "(${query.description})" else "(no description)"
                    val type = "of type ${query.type.simpleName}"
                    throw InvalidStateException(
                        "Request to ${route.name} is missing required query parameter \"${query.name}\" $description $type"
                    )
                }
            }

            // Create a secondary listener that writes responses to the caller before forwarding to the original one.
            val listener = object: RouteListener {
                override fun onStart(eventLoop: EventLoop, route: Route) = 0L
                override fun onSucceed(route: RouteContext, result: Any?) {
                    val writerIndex = target.writerIndex()
                    try {
                        target.writeByte(ResponseCode.Success.ordinal)
                        writeBinary(result, target)
                        f()
                        listener?.onSucceed(route, result)
                    } catch(e: Throwable) {
                        target.writerIndex(writerIndex)
                        mapError(e, target, f)
                        listener?.onFail(route, e)
                    }
                }
                override fun onFail(route: RouteContext, reason: Throwable?) {
                    mapError(reason, target, f)
                    listener?.onFail(route, reason)
                }
            }

            // Run the route handler.
            val routeContext = RouteContext(context, remote, eventLoop, route, params, queries, callId)
            try {
                route.handler(routeContext, listener)
            } catch(e: Throwable) {
                mapError(e, target, f)
                this.listener?.onFail(routeContext, e)
            }
        } catch(e: Throwable) {
            val error = convertDecodeError(e)
            mapError(error, target, f)

            // We don't have the call parameters here, so we just send a route context without them.
            listener?.onFail(RouteContext(context, remote, eventLoop, route, emptyArray(), emptyArray(), callId), error)
        }
    }

    fun mapError(error: Throwable?, target: ByteBuf, f: () -> Unit) = when(error) {
        is InvalidStateException -> error(ResponseCode.InvalidArgs, error.message ?: "bad request", target, f)
        is UnauthorizedException -> error(ResponseCode.NoPermission, error.message ?: "forbidden", target, f)
        is NotFoundException -> error(ResponseCode.NotFound, error.message ?: "not found", target, f)
        else -> error(ResponseCode.InternalError, "internal error", target, f)
    }

    fun error(code: ResponseCode, desc: String, target: ByteBuf, f: () -> Unit) {
        target.writeByte(code.ordinal)
        target.writeString(desc)
        f()
    }

    fun convertDecodeError(error: Throwable?) = when(error) {
        is InvalidStateException -> error
        else -> InvalidStateException("invalid call parameter")
    }
}