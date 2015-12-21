package com.rimmer.metrics.server

import com.rimmer.metrics.Sender
import com.rimmer.metrics.Statistic
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import org.joda.time.DateTime

class ServerSender: AbstractVerticle(), Sender {
    var bus: EventBus? = null

    override fun start() {
        bus = vertx.eventBus()
    }

    override fun sendStatistic(path: String, time: DateTime, stat: Statistic) {
        bus?.send(statsAggregator, StatPacket(path, "unknown", time, 0L, emptyList()))
    }

    override fun sendProfile(path: String, time: DateTime, start: Long, end: Long, events: List<Event>) {

    }

    override fun sendError(path: String, time: DateTime, reason: String) {
        bus?.send(errorAggregator, ErrorPacket(path, time, reason, reason, emptyMap()))
    }
}