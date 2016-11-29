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
import com.rimmer.yttrium.server.http.checkArgs
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import java.net.InetSocketAddress
import java.util.*

enum class ResponseCode {
    Success, NoRoute, NotFound, InvalidArgs, NoPermission, InternalError, MiscError
}

fun routeHash(r: Route) = routeHash(r.name, r.version)
fun routeHash(name: String, version: Int = 0) = name.hashCode() + version * 3452056

class BinaryRoute(
    val route: Route,
    val argCount: Int,
    val readers: Array<(ByteBuf) -> Any>,
    val map: ByteArray
)

class BinaryRouter(
    val router: Router,
    val listener: RouteListener? = null
): (ChannelHandlerContext, ByteBuf, ByteBuf, () -> Unit) -> Unit {
    private val segmentMap = router.routes.associateBy(::routeHash).mapValues {
        val args = it.value.args
        val readers = ArrayList<(ByteBuf) -> Any>()
        val map = ArrayList<Byte>()

        args.forEachIndexed { i, it ->
            if(it.visibility.exported) {
                readers.add(it.reader!!.fromBinary)
                map.add(i.toByte())
            }
        }

        BinaryRoute(it.value, args.size, readers.toTypedArray(), map.toByteArray())
    }

    override fun invoke(context: ChannelHandlerContext, source: ByteBuf, target: ByteBuf, f: () -> Unit) {
        val remote = (context.channel().remoteAddress() as? InetSocketAddress)?.hostName ?: ""
        val id = source.readVarInt()
        val binaryRoute = segmentMap[id]
        if(binaryRoute == null) {
            if(source.readableBytes() > 0) {
                source.readObject { false }
            }
            error(ResponseCode.NoRoute, "Route $id not found", target, f)
            return
        }

        val route = binaryRoute.route
        val eventLoop = context.channel().eventLoop()
        val callData = listener?.onStart(eventLoop, route)

        val params = arrayOfNulls<Any>(binaryRoute.argCount)
        val writer = route.writer
        val readers = binaryRoute.readers
        val map = binaryRoute.map
        val max = map.size

        // We can't really detect specific problems in the call parameters
        // without making everything really complicated,
        // so if the decoding fails we just return a simple error.
        try {
            if(source.readableBytes() > 0) {
                source.readObject {
                    val handled = it < max
                    if(handled) {
                        val i = map[it].toInt()
                        params[i] = readers[i](source)
                    }
                    handled
                }
            }

            checkArgs(route, params, null)

            // Create a secondary listener that writes responses to the caller before forwarding to the original one.
            val listener = object: RouteListener {
                override fun onStart(eventLoop: EventLoop, route: Route) = null
                override fun onSucceed(route: RouteContext, result: Any?, data: Any?) {
                    val writerIndex = target.writerIndex()
                    try {
                        target.writeByte(ResponseCode.Success.ordinal)
                        writeBinary(result, writer, target)
                        f()
                        listener?.onSucceed(route, result, route.listenerData)
                    } catch(e: Throwable) {
                        target.writerIndex(writerIndex)
                        mapError(e, target, f)
                        listener?.onFail(route, e, route.listenerData)
                    }
                }
                override fun onFail(route: RouteContext, reason: Throwable?, data: Any?) {
                    mapError(reason, target, f)
                    listener?.onFail(route, reason, route.listenerData)
                }
            }

            // Run the route handler.
            val routeContext = RouteContext(context, remote, eventLoop, route, params, callData, true)
            try {
                route.handler(routeContext, listener)
            } catch(e: Throwable) {
                mapError(e, target, f)
                this.listener?.onFail(routeContext, e, callData)
            }
        } catch(e: Throwable) {
            val error = convertDecodeError(e)
            mapError(error, target, f)

            // We don't have the call parameters here, so we just send a route context without them.
            val routeContext = RouteContext(context, remote, eventLoop, route, emptyArray(), callData, true)
            listener?.onFail(routeContext, e, callData)
        }
    }

    fun mapError(error: Throwable?, target: ByteBuf, f: () -> Unit) = when(error) {
        is InvalidStateException -> error(ResponseCode.InvalidArgs, error.message ?: "bad_request", target, f)
        is UnauthorizedException -> error(ResponseCode.NoPermission, error.message ?: "forbidden", target, f)
        is NotFoundException -> error(ResponseCode.NotFound, error.message ?: "not_found", target, f)
        is HttpException -> error(ResponseCode.MiscError, error.message ?: "error", target, f)
        else -> error(ResponseCode.InternalError, "internal_error", target, f)
    }

    fun error(code: ResponseCode, desc: String, target: ByteBuf, f: () -> Unit) {
        target.writeByte(code.ordinal)
        target.writeString(desc)
        f()
    }

    fun convertDecodeError(error: Throwable?) = when(error) {
        is InvalidStateException -> error
        else -> InvalidStateException("invalid_call")
    }
}