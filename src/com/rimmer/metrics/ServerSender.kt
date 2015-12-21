package com.rimmer.metrics

import com.rimmer.metrics.server.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus

class ServerSender: AbstractVerticle(), Sender {
    var bus: EventBus? = null

    override fun start() {
        bus = vertx.eventBus()
    }

    override fun sendStatistic(stat: StatPacket) {
        bus?.send(statsAggregator, stat)
    }

    override fun sendProfile(profile: ProfilePacket) {
        bus?.send(profileAggregator, profile)
    }

    override fun sendError(error: ErrorPacket) {
        bus?.send(errorAggregator, error)
    }
}