package com.rimmer.yttrium.router

import java.lang.reflect.Type
import java.util.*
import java.util.regex.Pattern

/** Interface that can be used to modify the signature of a route. */
interface Modifier {
    /**
     * Adds a path segment to this route.
     * @param equivalence The equivalence of the added segment. This should have the form "/abc/​*​/def".
     * @param swagger The segment to add, in swagger format.
     * This should have the form "/abc/{param}/def" and can also include implicit parameters.
     * @param parameters The parameters that will be added to the path, in order.
     * The number of parameters must be equal to the number of stars in the equivalence path.
     * Path parameters are always required.
     */
    fun addPath(equivalence: String, swagger: String, parameters: List<Parameter>)

    /**
     * Adds a query parameter to this route.
     * @param name The parameter name.
     * @param type The type this parameter should be converted to.
     * @param required If set, the route will fail if this parameter is not provided.
     */
    fun addQuery(name: String, type: Type, required: Boolean)
}

/**
 * Route modifier implementation.
 * This is used to build up the signature and parameters of a route.
 */
class RouteModifier(path: String): Modifier {
    val pathParams = ArrayList<Parameter>()
    val queryParams = ArrayList<Parameter>()

    var swaggerPath = createSwaggerPath(path)
    var equivalencePath = createEquivalencePath(path)

    override fun addPath(equivalence: String, swagger: String, parameters: List<Parameter>) {
        this.equivalencePath += equivalence
        this.swaggerPath += swagger
        pathParams.addAll(parameters)
    }

    override fun addQuery(name: String, type: Type, required: Boolean) {
        queryParams.add(Parameter(name, type, required))
    }
}

/** Converts the provided path string into Swagger format. */
private fun createSwaggerPath(path: String): String {
    // Check if the path contains any parameter bindings.
    // If so, we need to generate capture groups with a regex.
    return if(path.indexOf(':') != -1) {
        val string = StringBuffer()

        // Generate a set of groups using a regex.
        val matcher = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(path)
        while(matcher.find()) {
            val group = matcher.group().substring(1)
            matcher.appendReplacement(string, "{$group}")
        }
        matcher.appendTail(string)
        string.toString()
    } else {
        path
    }
}

/** Creates a path string that can be compared for equivalence. */
private fun createEquivalencePath(path: String): String {
    return if(path.indexOf(':') != -1) {
        val matcher = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(path)
        matcher.replaceAll("*")
    } else {
        path
    }
}