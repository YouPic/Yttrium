package com.rimmer.yttrium.router

import com.rimmer.yttrium.Task

fun routeHandler(
    route: Route,
    plugins: List<RoutePlugin>,
    pathBindings: IntArray,
    queryBindings: IntArray,
    firstQuery: Int,
    argumentCount: Int,
    call: RouteContext.(Array<Any?>) -> Task<*>
) = { context: RouteContext, listener: RouteListener ->
    val listenerId = listener.onStart(context.eventLoop, route)

    try {
        val arguments = arrayOfNulls<Any?>(argumentCount)

        // Add the context-provided arguments.
        var pathIndex = 0
        while(pathIndex < pathBindings.size) {
            arguments[pathBindings[pathIndex]] = context.pathParameters[pathIndex]
            pathIndex++
        }

        var queryIndex = 0
        while(queryIndex < queryBindings.size) {
            arguments[queryBindings[queryIndex]] = context.queryParameters[queryIndex + firstQuery]
            queryIndex++
        }

        // Let each plugin modify the result as needed.
        // After each plugin has run, we send the final result to the route listener.
        fun modifyResult(plugin: Iterator<RoutePlugin>, result: Any?) {
            if (plugin.hasNext()) {
                val p = plugin.next()
                p.plugin.modifyResult(p.context, context, result) { r, e ->
                    if(e == null) {
                        modifyResult(plugin, r!!)
                    } else {
                        listener.onFail(context, e)
                    }
                }
            } else {
                listener.onSucceed(context, result)
            }
        }

        // Let each plugin modify the call as needed.
        // After each plugin has run, we call the route handler.
        fun modifyCall(plugin: Iterator<RoutePlugin>, args: Array<Any?>) {
            if (plugin.hasNext()) {
                val p = plugin.next()
                p.plugin.modifyCall(p.context, context, args) { e ->
                    if(e == null) {
                        modifyCall(plugin, args)
                    } else {
                        listener.onFail(context, e)
                    }
                }
            } else {
                // Sometimes the call handler itself can throw exceptions, which we need to handle correctly.
                // Otherwise we risk compromising our state, since exceptions can go far back in the continuation chain.
                try {
                    context.call(args).handler = { r: Any?, e: Throwable? ->
                        if (e == null) {
                            modifyResult(plugins.iterator(), r)
                        } else {
                            listener.onFail(context, e)
                        }
                    }
                } catch(e: Throwable) {
                    listener.onFail(context, e)
                }
            }
        }

        modifyCall(plugins.iterator(), arguments)
    } catch(e: Throwable) {
        listener.onFail(context, e)
    }
}

