package com.rimmer.yttrium.metrics

import com.rimmer.metrics.Metrics
import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.NotFoundException
import com.rimmer.yttrium.UnauthorizedException
import com.rimmer.yttrium.router.Route
import com.rimmer.yttrium.router.RouteContext
import com.rimmer.yttrium.router.RouteListener
import io.netty.channel.EventLoop

class MetricsListener(val metrics: Metrics, val category: String, val next: RouteListener?): RouteListener {
    override fun onStart(eventLoop: EventLoop, route: Route): Long {
        val id = metrics.start(route.name, category).toLong()
        next?.onStart(eventLoop, route)
        return id
    }

    override fun onSucceed(route: RouteContext, result: Any?) {
        metrics.finish(route.id.toInt())
        next?.onSucceed(route, result)
    }

    override fun onFail(route: RouteContext, reason: Throwable?) {
        val wasError = when(reason) {
            is InvalidStateException -> false
            is NotFoundException -> false
            is UnauthorizedException -> false
            else -> true
        }

        metrics.fail(route.id.toInt(), wasError, reason?.message ?: "")
        next?.onFail(route, reason)
    }
}