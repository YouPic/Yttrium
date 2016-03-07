package com.rimmer.yttrium.router

import com.rimmer.yttrium.router.plugin.Plugin
import java.util.*

class RoutePlugin(val plugin: Plugin<in Any>, val context: Any)

class Router(val plugins: List<Plugin<in Any>>) {
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
        funSegments: Iterable<PathSegment>,
        funQueries: Iterable<BuilderQuery>,
        types: Array<Class<*>>,
        result: Class<*>,
        call: RouteContext.(Array<Any?>) -> Future<*>
    ) {
        val segments = funSegments.toMutableList()
        val queries = ArrayList<RouteQuery>()
        val providers = Array(types.size) { false }

        val modifier = object: RouteModifier {
            override val parameterTypes: Array<Class<*>> get() = types
            override fun provideParameter(index: Int) { providers[index] = true }

            override fun addPath(s: List<PathSegment>): Int {
                val id = segments.sumBy { if(it.type != null) 1 else 0 }
                segments.addAll(s)
                return id
            }

            override fun addArg(name: String, type: Class<*>, description: String): Int {
                val hash = name.hashCode()
                val id = queries.size
                queries.add(RouteQuery(name, hash, type, false, null, description))
                return id
            }

            override fun addOptional(name: String, type: Class<*>, default: Any?, description: String): Int {
                val hash = name.hashCode()
                val id = queries.size
                queries.add(RouteQuery(name, hash, type, true, default, description))
                return id
            }
        }

        // Find the plugins that will be applied to this route.
        val usedPlugins = ArrayList<RoutePlugin>()
        for(p in plugins) {
            val context = p.isUsed(modifier, result, properties)
            if(context != null) {
                usedPlugins.add(RoutePlugin(p, context))
                p.modifyRoute(context, modifier)
            }
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
        val typedSegments = funSegments.filter { it.type != null }.toTypedArray()
        if(providers.size < typedSegments.size) {
            throw IllegalArgumentException("Each path parameter must have a corresponding function argument.")
        }

        val pathBindings = ArrayList<Int>()
        typedSegments.forEachIndexed { i, segment ->
            if(providers[i]) {
                throw IllegalArgumentException("Defined path parameter ${segment.name} cannot be provided a plugin.")
            }

            pathBindings.add(i)
            swaggerRoute.parameters.add(
                Swagger.Parameter(segment.name, Swagger.ParameterType.Path, "", types[i], false)
            )
        }

        // Create a list of query parameter -> handler parameter bindings.
        // Arguments that are already provided by a plugin are presumed to be used by that plugin.
        val firstQuery = typedSegments.size
        val queryBindings = ArrayList<Int>()
        funQueries.forEachIndexed { i, q ->
            val index = firstQuery + i
            if(!providers[index]) {
                queryBindings.add(index)
                queries.add(RouteQuery(q.name, q.name.hashCode(), types[index], q.optional, q.default, q.description))
                swaggerRoute.parameters.add(
                    Swagger.Parameter(q.name, Swagger.ParameterType.Query, q.description, types[index], q.default != null)
                )
            }
        }

        // Create the route handler.
        val name = "$method ${swaggerRoute.info.path}"
        val inputSegments = segments.filter { it.type != null }.toTypedArray()
        val route = Route(name, method, version, segments.toTypedArray(), inputSegments, queries.toTypedArray())

        route.handler = routeHandler(
            route,
            usedPlugins,
            pathBindings.toIntArray(),
            queryBindings.toIntArray(),
            types.size,
            call
        )
        routes.add(route)
    }

    fun addRoute(desc: RouteBuilder, call: RouteContext.(Array<Any?>) -> Future<*>, types: Array<Class<*>>, result: Class<*>) {
        addRoute(desc.method, desc.version, desc.properties, buildSegments(desc.path, types), desc.queries, types, result, call)
    }

    fun get(path: String, version: Int = 0) = RouteBuilder(this, HttpMethod.GET, path, version)
    fun post(path: String, version: Int = 0) = RouteBuilder(this, HttpMethod.POST, path, version)
    fun delete(path: String) = RouteBuilder(this, HttpMethod.DELETE, path, 0)
    fun put(path: String) = RouteBuilder(this, HttpMethod.PUT, path, 0)
}