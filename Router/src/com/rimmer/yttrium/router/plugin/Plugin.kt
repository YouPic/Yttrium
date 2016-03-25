package com.rimmer.yttrium.router.plugin

import com.rimmer.yttrium.router.*
import java.lang.reflect.Type

/** Defines a Router plugin for using custom annotations and parameter types in routes. */
interface Plugin<Context> {
    /**
     * Checks if this plugin is used for the provided parameters and annotations.
     * If it is used a context object is returned. If not, null is returned and the plugin won't be used.
     */
    fun isUsed(route: RouteModifier, returnType: Type, properties: Iterable<RouteProperty>): Context?

    /**
     * Modifies the route path and adds any parameters needed for this plugin.
     * This is called once at route creation time.
     */
    fun modifyRoute(context: Context, modifier: RouteModifier) {}

    /**
     * Modifies the swagger data for this route, adding any parameters for this plugin.
     * This is called once at route creation time.
     */
    fun modifySwagger(context: Context, route: Swagger.Route) {}

    /**
     * Adds any function parameters needed for this plugin before calling the route handler.
     * This is called for every request to this route.
     * When done, call the provided callback to continue the route request.
     * @param arguments A list of arguments with the same order as in isUsed.
     * The plugin should set any arguments it indicated itself as a provider for.
     */
    fun modifyCall(context: Context, route: RouteContext, arguments: Array<Any?>, f: (Throwable?) -> Unit) = f(null)

    /**
     * Modifies the result returned by the route handler if needed.
     * When done, call the provided callback to continue the route request.
     */
    fun modifyResult(context: Context, route: RouteContext, result: Any?, f: (Any?, Throwable?) -> Unit) = f(result, null)
}
