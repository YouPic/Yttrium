package com.rimmer.yttrium.router

import com.rimmer.yttrium.Task
import com.rimmer.yttrium.router.listener.RouteListener

class RouteHandler(
    val plugins: List<RoutePlugin>,
    val call: RouteContext.(Array<Any?>) -> Task<*>
): (RouteContext, RouteListener) -> Unit {
    override fun invoke(context: RouteContext, listener: RouteListener) {
        val data = listener.onStart(context.eventLoop, context.route)

        try {
            modifyCall(plugins.iterator(), context, listener, data)
        } catch(e: Throwable) {
            listener.onFail(context, e, data)
        }
    }

    // Let each plugin modify the result as needed.
    // After each plugin has run, we send the final result to the route listener.
    fun modifyResult(plugin: Iterator<RoutePlugin>, context: RouteContext, listener: RouteListener, data: Any?, result: Any?) {
        if(plugin.hasNext()) {
            val p = plugin.next()
            p.plugin.modifyResult(p.context, context, result) { r, e ->
                if(e == null) {
                    modifyResult(plugin, context, listener, data, r!!)
                } else {
                    listener.onFail(context, e, data)
                }
            }
        } else {
            listener.onSucceed(context, result, data)
        }
    }

    // Let each plugin modify the call as needed.
    // After each plugin has run, we call the route handler.
    fun modifyCall(plugin: Iterator<RoutePlugin>, context: RouteContext, listener: RouteListener, data: Any?) {
        if(plugin.hasNext()) {
            val p = plugin.next()
            p.plugin.modifyCall(p.context, context) { e ->
                if(e == null) {
                    modifyCall(plugin, context, listener, data)
                } else {
                    listener.onFail(context, e, data)
                }
            }
        } else {
            // Sometimes the call handler itself can throw exceptions, which we need to handle correctly.
            // Otherwise we risk compromising our state, since exceptions can go far back in the continuation chain.
            try {
                context.call(context.parameters).handler = { r: Any?, e: Throwable? ->
                    if(e == null) {
                        modifyResult(plugins.iterator(), context, listener, data, r)
                    } else {
                        listener.onFail(context, e, data)
                    }
                }
            } catch(e: Throwable) {
                listener.onFail(context, e, data)
            }
        }
    }
}
