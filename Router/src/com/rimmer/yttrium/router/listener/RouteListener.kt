package com.rimmer.yttrium.router.listener

import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.RouteContext
import io.netty.channel.EventLoop

/**
 * When sent to a route handler, this will be called with progress information about the route.
 * Can be used to collect metrics or display debug information.
 */
interface RouteListener {
    /**
     * This is called whenever a route call starts.
     * @return A listener id that will be sent to any followup calls for this route.
     */
    fun onStart(eventLoop: EventLoop, route: Route): Long

    /**
     * This is called whenever a route call succeeds.
     * @param route The context for this route, including the call id returned by onStart.
     * @param result The returned result of the route.
     */
    fun onSucceed(route: RouteContext, result: Any?)

    /**
     * This is called whenever a route call fails.
     * @param route The context for this route, including the call id returned by onStart.
     * @param reason The reason this call failed, if any.
     */
    fun onFail(route: RouteContext, reason: Throwable?)
}