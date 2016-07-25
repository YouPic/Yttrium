package com.rimmer.yttrium.server.binary

import com.rimmer.yttrium.HttpException
import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.Router
import com.rimmer.yttrium.router.listener.RouteListener
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.server.http.checkQueries
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import java.net.InetSocketAddress

enum class ResponseCode {
    Success, NoRoute, NotFound, InvalidArgs, NoPermission, InternalError, MiscError
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
        val paramCount = params.size
        val queries = arrayOfNulls<Any>(route.queries.size)
        val writer = route.writer

        // We can't really detect specific problems in the call parameters
        // without making everything really complicated,
        // so if the decoding fails we just return a simple error.
        try {
            source.readObject {
                if(it > paramCount + queries.size) {
                    false
                } else if(it > paramCount) {
                    val i = it - paramCount
                    queries[i] = route.queries[i].reader!!.fromBinary(source)
                    true
                } else {
                    params[it] = route.typedSegments[it].reader!!.fromBinary(source)
                    true
                }
            }

            checkQueries(route, queries, null)

            // Create a secondary listener that writes responses to the caller before forwarding to the original one.
            val listener = object: RouteListener {
                override fun onStart(eventLoop: EventLoop, route: Route) = 0L
                override fun onSucceed(route: RouteContext, result: Any?) {
                    val writerIndex = target.writerIndex()
                    try {
                        target.writeByte(ResponseCode.Success.ordinal)
                        writeBinary(result, writer, target)
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
        is HttpException -> error(ResponseCode.MiscError, error.message ?: "error", target, f)
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