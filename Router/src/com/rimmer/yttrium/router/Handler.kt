package com.rimmer.yttrium.router

fun routeHandler(
    route: Route,
    plugins: List<RoutePlugin>,
    pathBindings: IntArray,
    queryBindings: IntArray,
    argumentCount: Int,
    call: RouteContext.(Array<Any?>) -> Future<in Any>
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

        // Let each plugin modify the call as needed.
        for(p in plugins) {
            p.plugin.modifyCall(p.context, context, arguments)
        }

        // Let each plugin modify the result as needed.
        // After each plugin has run, we send the final result to the route listener.
        fun modify(plugin: Iterator<RoutePlugin>, result: Any?) {
            if (plugin.hasNext()) {
                val p = plugin.next()
                p.plugin.modifyResult(p.context, context, result) {
                    modify(plugin, it)
                }
            } else {
                listener.onSucceed(listenerId, result)
            }
        }

        // Call the actual api function and send its result to the plugin modifier.
        context.call(arguments).then { modify(plugins.iterator(), it) }
    } catch(e: Throwable) {
        listener.onFail(listenerId, e)
    }
}

