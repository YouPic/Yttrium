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
        val json = XContentFactory.jsonBuilder()
        json.startObject().field("path", path).field("time", time)
        json.startArray("events")
        events.forEach {
            json.startObject()
                .field("eventType", it.event)
                .field("eventKind", it.type)
                .field("time", it.startDate)
                .field("start", it.startTime)
                .field("end", it.endTime)
            .endObject()
        }
        json.endArray()
        json.endObject()

        search.prepareIndex("metrics", "profile").setSource(json).execute()
    }

    override fun sendStatistic(path: String, time: DateTime, stat: Statistic) {
        val json = XContentFactory.jsonBuilder()
        .startObject()
            .field("path", path)
            .field("time", time)
            .startObject("stat")
                .field("median", stat.median)
                .field("average", stat.average)
                .field("average95", stat.average95)
                .field("average99", stat.average99)
                .field("max", stat.max)
                .field("min", stat.min)
                .field("count", stat.count)
            .endObject()
        .endObject()

        search.prepareIndex("metrics", "stat").setSource(json).execute()
    }
}