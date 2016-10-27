package com.rimmer.yttrium.router

import com.rimmer.yttrium.Context
import com.rimmer.yttrium.Task
import com.rimmer.yttrium.router.listener.RouteListener
import com.rimmer.yttrium.serialize.Reader
import com.rimmer.yttrium.serialize.Writer
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop

/** The supported http call methods. */
enum class HttpMethod {GET, POST, DELETE, PUT}

/**
 * Defines where a route parameter is used.
 * Default indicates that the parameter is exported and sent to the route handler.
 * Internal indicates that the parameter is created by a plugin and not exported.
 * External indicates that the parameter is exported, but used by a plugin only.
 */
enum class ArgVisibility {
    Default, Internal, External;

    val exported: Boolean get() = this === External || this === Default
}

/**
 * Represents a query parameter within a route.
 * @param name The parameter name as it appears in the url.
 * @param visibility Whether this parameter should be parsed from the call data.
 * @param isPath Set if this argument is part of the route path.
 * @param reader A reader for the query target type.
 * @param default The default value for this parameter, if it is optional.
 */
data class Arg(
    val name: String,
    val visibility: ArgVisibility,
    val isPath: Boolean,
    val optional: Boolean,
    val default: Any?,
    val type: Class<*>,
    val reader: Reader?
)

/**
 * Represents one segment in the url of a route.
 * @param name For string segments, contains the segment value. For parsed segments, contains the parameter name.
 * @param arg If this is a parsed segment, contains the corresponding route argument.
 * @param argIndex If this is a parsed segment, contains the parameter index.
 */
class Segment(val name: String, val arg: Arg?, val argIndex: Int)

/**
 * Contains information about a callable route in this server.
 * @param name The route name as it will be displayed in logs, metrics, etc.
 * @param method The HTTP calling method for this route.
 * @param version The route version - this allows using equivalent routes with different parameters.
 * @param segments The route path as it will be called through HTTP.
 * @param args The arguments for this route, including arguments defined through the path.
 * @param handler Takes request information for the route and executes it.
 */
class Route(
    val name: String,
    val method: HttpMethod,
    val version: Int,
    val segments: Array<Segment>,
    val typedSegments: Array<Segment>,
    val args: Array<Arg>,
    val writer: Writer<*>?,
    val bodyQuery: Int?,
    var handler: (RouteContext, RouteListener) -> Unit
)

/** Interface that can be used by plugins to modify the signature of a route. */
interface RouteModifier {
    /** The currently defined route segments. */
    val segments: List<Segment>

    /** The currently defined route arguments.  */
    val args: List<Arg>

    /**
     * Indicates that this plugin will provide this parameter to the route handler.
     * It will be ignored by the query parameter builder.
     */
    fun provideFunArg(index: Int)

    /**
     * Adds to the calling url of this route.
     * @param segments A list of segments to add - this can contain both string and parameter segments.
     * @return The id of the created parameter for the first typed segment. This can be used to retrieve it in modifyCall.
     */
    fun addPath(segments: List<PathSegment>): Int

    /**
     * Adds an additional query parameter.
     * @return The id of the created query. This can be used to retrieve it in modifyCall.
     */
    fun addArg(name: String, type: Class<*>, reader: Reader?): Int

    /**
     * Adds an additional optional query parameter.
     * @return The id of the created query. This can be used to retrieve it in modifyCall.
     */
    fun addOptional(name: String, type: Class<*>, reader: Reader?, default: Any? = null): Int

    /** Returns the index of the first function argument of the provided type, or null. */
    fun hasFunArg(type: Class<*>): Int? {
        val i = args.indexOfFirst { it.type === type }
        return if(i == -1) null else i
    }

    /** If a parameter of the requested type exists, provide it and return its index. */
    fun provideFunArg(type: Class<*>): Int? {
        val i = hasFunArg(type) ?: return null
        provideFunArg(i)
        return i
    }
}

/**
 * Defines a generic property applied on a route.
 * Properties can be used to send additional data to plugins.
 */
class RouteProperty(val name: String, val value: Any)

/**
 * Context information that is sent to route handlers.
 * @param channel The channel this route is being executed for.
 * @param sourceIp The source ip this request originated from. This may be different from the channel ip.
 * @param eventLoop The current event loop.
 * @param route The route being executed.
 * @param pathParameters The path parameters that were sent to the route.
 * @param queryParameters The query parameters that were sent to the route.
 * @param listenerData Data used by the first listener.
 * @param channelPrivate If set, the channel for this context will only be used for communication with a single client.
 */
class RouteContext(
    val channel: ChannelHandlerContext,
    val sourceIp: String,
    eventLoop: EventLoop,
    val route: Route,
    val parameters: Array<Any?>,
    listenerData: Any?,
    val channelPrivate: Boolean
): Context(eventLoop, listenerData)
