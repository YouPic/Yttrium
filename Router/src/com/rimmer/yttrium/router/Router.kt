package com.rimmer.yttrium.router

import com.rimmer.yttrium.router.plugin.Plugin
import java.util.*
import java.util.regex.Pattern
import kotlin.reflect.*
import kotlin.reflect.jvm.javaType

class RouteBuilder(val swagger: Swagger, val plugins: List<Plugin<in Any>>, val listener: RouteListener? = null, val router: (Route) -> Unit) {
    /**
     * Walks through the provided class and creates a router binding for each function that has an http annotation.
     * Each function is then called whenever a matching request is made to the provided router.
     * Valid http annotations and their use can be found in Types.kt.
     * @param type The class to create routes for.
     * @return A swagger definition of the routes that were built.
     */
    fun buildRoutes(type: KClass<*>) {
        val swaggerCategory = swagger.addCategory(type.simpleName ?: "")

        // Create a list of all routes found. This is used to sort them later.
        val routeList = ArrayList<Route>()

        // Create an instance of the type, which will be used to call its methods.
        val instance = createInstance(type)

        // Check for each member function if it can be used as a path.
        for(m in type.memberFunctions) {
            buildRoute(m, swaggerCategory, routeList, instance)
        }

        // Create a handler for each path.
        routeList.sortedBy {-it.version}.forEach {router(it)}
    }

    private fun buildRoute(function: KFunction<*>, category: Swagger.Category, routes: MutableList<Route>, instance: Any?) {
        // If the function has an annotation that corresponds to an http path, we create a mapping.
        val (annotation, http, path) = findHttpMethod(function, plugins) ?: return
        val groups = createGroups(path)
        val modifier = RouteModifier(path)

        // Check if this is a versioned implementation of the path.
        val version = getVersionDate(function.name) ?: throw IllegalArgumentException("Invalid version format on the api path ${function.name} ($path)")

        /*
         * Take the full set of parameters and filter out different types.
         * The final set left is used as query parameters.
         */

        val annotations = function.annotations.toMutableList()
        val parameters = function.parameters.toMutableList()
        val returnType = function.returnType.javaType

        // Check if the function needs a class instance.
        val self = parameters.find {it.kind == KParameter.Kind.INSTANCE}
        if(self != null) {
            parameters.remove(self)
        }

        // Find the plugins that will be applied to this route.
        val usedPlugins = ArrayList<RoutePlugin>()
        for(p in plugins) {
            val context = p.isUsed(annotation, parameters, annotations, returnType)
            if(context != null) {
                usedPlugins.add(RoutePlugin(p, context, modifier.queryParams.size, modifier.pathParams.size))
                p.modifyRoute(context, modifier)
            }
        }

        // Create the swagger route.
        val swaggerRoute = Swagger.Route(Swagger.PathInfo(modifier.swaggerPath, modifier.equivalencePath, category.name), http, version)
        swagger.addRoute(swaggerRoute, category)

        // Apply the plugins to swagger.
        for(p in usedPlugins) {
            p.plugin.modifySwagger(p.context, swaggerRoute)
        }

        // Create a set of mappings from path parameter to function parameter.
        parameters.filter {it.name in groups}.forEach {
            modifier.pathParams.add(Parameter(it.name ?: "", it.type.javaType, true, it))
            swaggerRoute.parameters.add(Swagger.Parameter(it.name ?: "", Swagger.ParameterType.Path, "", it.type.javaType, false))
            parameters.remove(it)
        }

        // Create a set of mappings from query parameter to function parameter.
        parameters.forEach {
            modifier.queryParams.add(Parameter(it.name ?: "", it.type.javaType, !it.isOptional, it))
            swaggerRoute.parameters.add(Swagger.Parameter(it.name ?: "", Swagger.ParameterType.Query, "", it.type.javaType, it.isOptional))
        }

        // Create the route handler.
        val route = Route(modifier.equivalencePath, http, version, modifier.pathParams, modifier.queryParams, null)
        val handler = RouteHandler(route, usedPlugins, instance, function, self, "$http ${modifier.swaggerPath}", listener)
        route.handler = handler
        routes.add(route)
    }

    private fun createInstance(type: KClass<*>) = try {
        type.primaryConstructor?.call()
    } catch(e: Exception) {
        throw IllegalArgumentException("The type ${type.simpleName} must have a constructor with no arguments")
    }
}

/** Retrieves a set of path parameter names from a path string. */
fun createGroups(path: String): Set<String> {
    // Check if the path contains any parameter bindings.
    // If so, we need to generate capture groups with a regex.
    return if(path.indexOf(':') != -1) {
        // Generate a set of groups using a regex.
        val groups = HashSet<String>()
        val matcher = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(path)
        while(matcher.find()) {
            val group = matcher.group().substring(1)
            if(!groups.add(group)) {
                throw IllegalArgumentException("Cannot use path parameter $group multiple times")
            }
        }
        groups
    } else {
        emptySet<String>()
    }
}

/** Finds an http method annotation for this function. If none are found, null is returned. */
private fun findHttpMethod(function: KFunction<*>, plugins: List<Plugin<*>>): Triple<Annotation, HttpMethod, String>? {
    var http: HttpMethod? = null
    var path: String? = null
    var annotation: Annotation? = null

    for(a in function.annotations) {
        var method = getHttpMethod(a)
        if(method == null) {
            for(p in plugins) {
                method = p.isMethod(a)
                if(method != null) break
            }
        }

        if(method != null) {
            http = method.first
            path = method.second
            annotation = a
            break
        }
    }

    if(annotation == null) return null

    return Triple(annotation, http!!, path!!)
}

/** Retrieves the http method from an http annotation. */
private fun getHttpMethod(annotation: Annotation) = when(annotation) {
    // Since annotations have no inheritance, we need to use a switch here.
    is Get -> Pair(HttpMethod.GET, annotation.path)
    is Post -> Pair(HttpMethod.POST, annotation.path)
    is Delete -> Pair(HttpMethod.DELETE, annotation.path)
    is Put -> Pair(HttpMethod.PUT, annotation.path)
    else -> null
}

/**
 * Returns the version date of a method or 0 if it is unversioned.
 * Returns null if the version is invalid.
 * The date is an integer with the format yyyymmdd
 */
private fun getVersionDate(name: String): Int? {
    if(name.find {it == '_'} != null) {
        try {
            val parts = name.split('_')
            if(parts.size != 4) {
                return null
            }

            // The last three parts form a date according to yyyy MM DD.
            // Check if each part is an integer an plausibly valid.
            val y = Integer.parseUnsignedInt(parts[1])
            val m = Integer.parseUnsignedInt(parts[2])
            val d = Integer.parseUnsignedInt(parts[3])

            // This throws an exception if the date is invalid.
            Calendar.getInstance().set(y, m, d)

            // Put together the correct integer format.
            return Integer.parseInt(parts[1] + parts[2] + parts[3])
        } catch(e: Exception) {
            return null
        }
    }

    // Unversioned.
    return 0
}