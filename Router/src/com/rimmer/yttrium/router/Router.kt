package com.rimmer.yttrium.router

import com.rimmer.yttrium.Task
import com.rimmer.yttrium.router.plugin.Plugin
import com.rimmer.yttrium.serialize.BodyContent
import com.rimmer.yttrium.serialize.Reader
import com.rimmer.yttrium.serialize.Writer
import java.util.*

class RoutePlugin(val plugin: Plugin<in Any>, val context: Any)

class BuilderQuery(
    val name: String, val optional: Boolean, val default: Any?,
    val description: String, val type: Class<*>, val reader: Reader?
)

class Router(plugins: List<Plugin<in Any>>) {
    val pluginMap = plugins.associateBy { it.name }
    val routes = ArrayList<Route>()

    fun addRoute(
        method: HttpMethod,
        version: Int,
        properties: List<RouteProperty>,
        funSegments: List<PathSegment>,
        funQueries: List<BuilderQuery>,
        plugins: List<Plugin<in Any>>,
        writer: Writer<*>?,
        call: RouteContext.(Array<Any?>) -> Task<*>
    ) {
        val args = ArrayList<Arg>()
        val segments = ArrayList<Segment>()
        funSegments.transformSegments(args, segments, ArgVisibility.Default)

        funQueries.forEach {
            args.add(Arg(it.name, ArgVisibility.Default, false, it.optional, it.default, it.type, it.reader))
        }

        // Create a route modifier for plugins.
        val modifier = object: RouteModifier {
            override val segments: List<Segment> get() = segments
            override val args: List<Arg> get() = args

            override fun provideFunArg(index: Int) {
                args[index] = args[index].copy(visibility = ArgVisibility.Internal)
            }

            override fun addPath(s: List<PathSegment>): Int {
                val id = args.size
                s.transformSegments(args, segments, ArgVisibility.External)
                return id
            }

            override fun addArg(name: String, type: Class<*>, reader: Reader?): Int {
                val id = args.size
                args.add(Arg(name, ArgVisibility.External, false, false, null, type, reader))
                return id
            }

            override fun addOptional(name: String, type: Class<*>, reader: Reader?, default: Any?): Int {
                val id = args.size
                args.add(Arg(name, ArgVisibility.External, false, true, default, type, reader))
                return id
            }
        }

        // Apply all plugins for this route.
        val usedPlugins = plugins.map { RoutePlugin(it, it.modifyRoute(modifier, properties)!!) }

        // If the segment list is still empty, we add a dummy element to simplify handler code.
        if(segments.isEmpty()) {
            segments.add(Segment("", null, -1))
        }

        // Create the route and handler.
        val name = "$method ${buildSwaggerPath(segments)}"
        val bodyQuery = args.indexOfFirst { it.type === BodyContent::class.java }

        routes.add(Route(
            name, method, version,
            segments.toTypedArray(),
            segments.filter { it.arg != null }.toTypedArray(),
            args.toTypedArray(),
            writer,
            if(bodyQuery == -1) null else bodyQuery,
            RouteHandler(usedPlugins, call)
        ))
    }
}

private fun Collection<PathSegment>.transformSegments(
    args: MutableList<Arg>,
    segments: MutableList<Segment>,
    visibility: ArgVisibility
) {
    forEach {
        if(it.type == null) {
            segments.add(Segment(it.name, null, -1))
        } else {
            val arg = Arg(it.name, visibility, true, false, null, it.type, it.reader)
            val i = args.size
            args.add(arg)
            segments.add(Segment(it.name, arg, i))
        }
    }
}