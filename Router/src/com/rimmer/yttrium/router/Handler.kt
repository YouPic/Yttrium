package com.rimmer.yttrium.router

import com.rimmer.yttrium.Task

fun routeHandler(
    route: Route,
    plugins: List<RoutePlugin>,
    pathBindings: IntArray,
    queryBindings: IntArray,
    argumentCount: Int,
    call: RouteContext.(Array<Any?>) -> Task<*>
) = { context: RouteContext, listener: RouteListener ->
    val listenerId = listener.onStart(route)

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
            arguments[queryBindings[queryIndex]] = context.queryParameters[queryIndex]
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
                        listener.onFail(listenerId, route, e)
                    }
                }
            } else {
                listener.onSucceed(listenerId, route, result)
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
                        listener.onFail(listenerId, route, e)
                    }
                }
            } else {
                context.call(args).onFinish {
                    modifyResult(plugins.iterator(), it)
                }.onFail {
                    listener.onFail(listenerId, route, it)
                }
            }
        }

        modifyCall(plugins.iterator(), arguments)
    } catch(e: Throwable) {
        listener.onFail(listenerId, route, e)
    }
}

