package com.rimmer.yttrium.router

import com.rimmer.yttrium.Task
import com.rimmer.yttrium.router.plugin.Plugin
import com.rimmer.yttrium.serialize.BodyContent
import com.rimmer.yttrium.serialize.Reader
import com.rimmer.yttrium.serialize.Writer
import com.rimmer.yttrium.serialize.unitReader
import java.util.*

class RoutePlugin(val plugin: Plugin<in Any>, val context: Any)

class BuilderQuery(
    val name: String, val optional: Boolean, val default: Any?,
    val description: String, val type: Class<*>, val reader: Reader?
)

class Router(plugins: List<Plugin<in Any>>) {
    val pluginMap = plugins.associateBy { it.name }
    val routes = ArrayList<Route>()
    val swagger = Swagger()
    var currentCategory = swagger.addCategory("None")

    inline fun category(name: String, paths: Router.() -> Unit) {
        val oldCategory = currentCategory
        currentCategory = swagger.addCategory(name)
        paths()
        currentCategory = oldCategory
    }

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
        val segments = funSegments.toMutableList()
        val queries = ArrayList<RouteQuery>()

        val typedSegments = funSegments.filter { it.reader !== null }.toTypedArray()
        val providers = Array(typedSegments.size + funQueries.size) { false }
        val types = (typedSegments.map { it.type!! } + funQueries.map { it.type }).toTypedArray()
        val readers = (typedSegments.map { it.reader } + funQueries.map { it.reader }).toTypedArray()

        val modifier = object: RouteModifier {
            override val parameterTypes: Array<Class<*>> get() = types
            override fun provideParameter(index: Int) { providers[index] = true }

            override fun addPath(s: List<PathSegment>): Int {
                val id = segments.sumBy { if(it.reader != null) 1 else 0 }
                segments.addAll(s)
                return id
            }

            override fun addArg(name: String, type: Class<*>, reader: Reader, description: String): Int {
                val hash = name.hashCode()
                val id = queries.size
                queries.add(RouteQuery(name, hash, type, reader, false, null, description))
                return id
            }

            override fun addOptional(name: String, type: Class<*>, reader: Reader, default: Any?, description: String): Int {
                val hash = name.hashCode()
                val id = queries.size
                queries.add(RouteQuery(name, hash, type, reader, true, default, description))
                return id
            }
        }

        // Find the plugins that will be applied to this route.
        val usedPlugins = ArrayList<RoutePlugin>()
        for(p in plugins) {
            val context = p.modifyRoute(modifier, properties)!!
            usedPlugins.add(RoutePlugin(p, context))
        }

        // Create the swagger route.
        val swaggerRoute = Swagger.Route(
            Swagger.PathInfo(buildSwaggerPath(segments), buildEquivalencePath(segments), currentCategory.name),
            method, version
        )
        swagger.addRoute(swaggerRoute, currentCategory)

        // Apply the plugins to swagger.
        for(p in usedPlugins) {
            p.plugin.modifySwagger(p.context, swaggerRoute)
        }

        // Create a list of path parameter -> handler parameter bindings.
        // Only use the original segments here - any segments added by plugins should be handled by those.
        if(providers.size < typedSegments.size) {
            throw IllegalArgumentException("Each path parameter must have a corresponding function argument.")
        }

        val pathBindings = ArrayList<Int>()
        typedSegments.forEachIndexed { i, segment ->
            if(providers[i]) {
                throw IllegalArgumentException("Defined path parameter ${segment.name} cannot be provided a plugin.")
            }

            pathBindings.add(i)
            swaggerRoute.parameters.add(Swagger.Parameter(
                segment.name, Swagger.ParameterType.Path, "", types[i], false
            ))
        }

        // Create a list of query parameter -> handler parameter bindings.
        // Arguments that are already provided by a plugin are presumed to be used by that plugin.
        val firstQuery = typedSegments.size
        val existingQueries = queries.size
        val queryBindings = ArrayList<Int>()
        funQueries.forEachIndexed { i, q ->
            val index = firstQuery + i
            if(!providers[index]) {
                queryBindings.add(index)
                queries.add(RouteQuery(
                    q.name, q.name.hashCode(), q.type, q.reader, q.optional, q.default, q.description
                ))
                swaggerRoute.parameters.add(Swagger.Parameter(
                    q.name, Swagger.ParameterType.Query, q.description, q.type, q.default !== null
                ))
            }
        }

        // If the segment list is empty, we have a root path.
        // Since the root still technically has one segment (an empty one) we add it ourselves.
        if(segments.isEmpty()) {
            segments.add(PathSegment("", null, null))
        }

        // Create the route handler.
        val name = "$method ${swaggerRoute.info.path}"
        val inputSegments = segments.filter { it.type !== null }.toTypedArray()
        val bodyQuery = queries.indexOfFirst { it.type === BodyContent::class.java }
        val route = Route(
            name, method, version,
            segments.toTypedArray(),
            inputSegments,
            queries.toTypedArray(),
            writer,
            if(bodyQuery == -1) null else bodyQuery
        )

        route.handler = routeHandler(
            route,
            usedPlugins,
            pathBindings.toIntArray(),
            queryBindings.toIntArray(),
            existingQueries,
            readers.size,
            call
        )
        routes.add(route)
    }
}