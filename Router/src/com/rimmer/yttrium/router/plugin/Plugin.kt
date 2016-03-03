package com.rimmer.yttrium.router.plugin

import com.rimmer.yttrium.router.HttpMethod
import com.rimmer.yttrium.router.Modifier
import com.rimmer.yttrium.router.Swagger
import java.lang.reflect.Type
import java.net.SocketAddress
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

/** Defines a Router plugin for using custom annotations and parameter types in routes. */
interface Plugin<Context> {
    /** If the provided annotation is a method annotation, this returns the corresponding http method and route path. */
    fun isMethod(annotation: Annotation): Pair<HttpMethod, String>? = null

    /**
     * Checks if this plugin is used for the provided parameters and annotations.
     * If it is used a context object is returned. If not, null is returned and the plugin won't be used.
     */
    fun isUsed(method: Annotation, parameters: MutableList<KParameter>, annotations: MutableList<Annotation>, returnType: Type): Context?

    /** Modifies the route path and adds any parameters needed for this plugin. */
    fun modifyRoute(context: Context, modifier: Modifier) {}

    /** Modifies the swagger data for this route, adding any parameters for this plugin. */
    fun modifySwagger(context: Context, route: Swagger.Route) {}

    /**
     * Adds any function parameters needed for this plugin before calling the route handler.
     * @param remote The host this request originated from.
     * @param pathParams A list of path parameters parsed for this route.
     * @param firstPath The first parameter in pathParams that is relevant for this plugin.
     * @param queryParams A list of query parameters parsed for this route.
     * @param firstQuery The fist parameter in queryParams that is relevant for this plugin.
     * @param arguments The parameter map for the route that should be filled.
     */
    fun modifyCall(
        context: Context,
        remote: SocketAddress,
        pathParams: List<Any>,
        firstPath: Int,
        queryParams: Array<Any?>,
        firstQuery: Int,
        arguments: MutableMap<KParameter, Any?>
    ) {}

    /** Modifies the result returned by the route handler if needed. */
    fun modifyResult(
        context: Context,
        pathParams: List<Any>,
        firstPath: Int,
        queryParams: Array<Any?>,
        firstQuery: Int,
        result: Any?
    ): Any? = result
}

/** Checks if an annotation is in a list and returns it. */
inline fun <reified T: Annotation> findAnnotation(annotations: List<Annotation>) = annotations.find {it is T} as? T

/**
 * Checks if a parameter of a certain type is in a list and returns it.
 * @param remove If set, the parameter is removed from the list.
 */
inline fun <reified T: Any> findParameter(parameters: MutableList<KParameter>, remove: Boolean = true): KParameter? {
    var i = 0
    var count = parameters.size
    while(i < count) {
        val p = parameters[i]
        if(p.type.javaType == T::class.java) {
            if(remove) {
                parameters.removeAt(i)
            }
            return p
        }
        i++
    }
    return null
}