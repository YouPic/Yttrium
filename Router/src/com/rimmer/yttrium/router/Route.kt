package com.rimmer.yttrium.router

import com.rimmer.yttrium.router.plugin.Plugin
import java.lang.reflect.Type
import kotlin.reflect.KParameter

enum class HttpMethod {GET, POST, DELETE, PUT}
val httpMethods = HttpMethod.values()

/** Describes a parameter to a route within the server. */
class Parameter(val name: String, val type: Type, val required: Boolean = true, val parameter: KParameter? = null)

/** Contains information about a callable route in this server. */
class Route(
    val equivalencePath: String,
    val method: HttpMethod,
    val version: Int,
    val pathParams: List<Parameter>,
    val queryParams: List<Parameter>,
    var handler: RouteHandler?
)

/** Describes how a router plugin should be called for a specific route. */
class RoutePlugin(val plugin: Plugin<in Any>, val context: Any, val firstQuery: Int, val firstPath: Int)

/**
 * When sent to a route handler, this will be called with progress information about the route.
 * Can be used to collect metrics or display debug information.
 */
interface RouteListener {
    /**
     * This is called whenever a route call starts.
     * @return A listener id that will be sent to any followup calls for this route.
     */
    fun onStart(routeName: String, route: Route): Long

    /**
     * This is called whenever a route call succeeds.
     * @param id The id that was previously returned by onStart.
     */
    fun onSucceed(id: Long)

    /**
     * This is called whenever a route call fails.
     * @param id The id that was previously returned by onStart.
     * @param wasError If set, the call failed due to a server error.
     * Otherwise, the call failed because of invalid input.
     * @param reason The reason this call failed, if any.
     */
    fun onFail(id: Long, wasError: Boolean, reason: Throwable?)
}