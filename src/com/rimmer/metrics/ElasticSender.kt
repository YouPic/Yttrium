package com.rimmer.metrics

import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentFactory
import org.joda.time.DateTime

/** A metrics sender implementation for ElasticSearch. */
class ElasticSender(val search: Client): Sender {
    override fun sendError(path: String, time: DateTime, reason: String) {
        search.prepareIndex("metrics", "error").setSource(
            XContentFactory.jsonBuilder().startObject()
                .field("path", path)
                .field("time", time)
                .field("reason", reason)
                .endObject()
        ).execute()
    }

    override fun sendProfile(path: String, time: DateTime, start: Long, end: Long, events: List<Event>) {
        search.prepareIndex("metrics", "profile").setSource(
            XContentFactory.jsonBuilder().startObject()
                .field("path", path)
                .field("time", time)
                .field("events", events)
                .endObject()
        ).execute()
    }

    override fun sendStatistic(path: String, time: DateTime, stat: Statistic) {
        search.prepareIndex("metrics", "stat").setSource(
            XContentFactory.jsonBuilder().startObject()
                .field("path", path)
                .field("time", time)
                .field("stat", stat)
                .endObject()
        ).execute()
    }
}