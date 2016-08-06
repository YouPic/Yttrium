package com.rimmer.yttrium.router.listener

import com.rimmer.metrics.Metrics
import com.rimmer.yttrium.HttpException
import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.RouteContext
import io.netty.channel.EventLoop
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

class MetricsListener(val metrics: Metrics, val category: String, val next: RouteListener?): RouteListener {
    override fun onStart(eventLoop: EventLoop, route: Route) =
        metrics.start(route.name, category, next?.onStart(eventLoop, route))

    override fun onSucceed(route: RouteContext, result: Any?, data: Any?) {
        if(data is Metrics.Call) {
            val nextData = data.nextData
            metrics.finish(data)
            next?.onSucceed(route, result, nextData)
        } else if(next !== null && data is Pair<*, *>) {
            next.onSucceed(route, result, data.second)
        } else {
            next?.onSucceed(route, result, null)
        }
    }

    override fun onFail(route: RouteContext, reason: Throwable?, data: Any?) {
        val text = reason.toString()
        val wasError = when(reason) {
            is InvalidStateException -> false
            is NotFoundException -> false
            is UnauthorizedException -> false
            is HttpException -> reason.errorCode >= 500
            else -> true
        }

        val writer = StringWriter()
        reason?.printStackTrace(PrintWriter(writer))

        if(data is Metrics.Call) {
            metrics.failCall(data, wasError, text, writer.toString())
        } else if(data is Pair<*, *>) {
            (data.first as? Metrics.Path)?.let {
                metrics.failPath(it, wasError, text, writer.toString())
            }
            next?.onFail(route, reason, data.second)
        } else {
            next?.onFail(route, reason, null)
        }
    }
}