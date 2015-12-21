package com.rimmer.metrics.server

import io.vertx.core.AbstractVerticle

class Server(val store: MetricStore): AbstractVerticle() {
    override fun start() {
        val bus = vertx.eventBus()
        bus.consumer<StatPacket>(statsAggregator) {
            store.onStat(it.body())
        }

        bus.consumer<ErrorPacket>(errorAggregator) {
            store.onError(it.body())
        }

        bus.consumer<ProfilePacket>(profileAggregator) {
            store.onProfile(it.body())
        }
    }
}