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

/** Call this to recreate the index. This removes any existing metrics data! */
fun createElasticIndex(search: org.elasticsearch.client.Client) {
    if(search.admin().indices().prepareExists("metrics").get().isExists) {
        search.admin().indices().prepareDelete("metrics").get()
    }

    val error = XContentFactory.jsonBuilder()
    .startObject()
        .startObject("error")
            .startObject("properties")
                .startObject("path").field("type", "string").field("index", "not_analyzed").endObject()
                .startObject("time").field("type", "date").endObject()
                .startObject("reason").field("type", "string").field("index", "not_analyzed").endObject()
            .endObject()
        .endObject()
    .endObject()

    val profile = XContentFactory.jsonBuilder()
    .startObject()
        .startObject("profile")
            .startObject("properties")
                .startObject("path").field("type", "string").field("index", "not_analyzed").endObject()
                .startObject("time").field("type", "date").endObject()
                .startObject("events")
                    .startObject("properties")
                        .startObject("eventType").field("type", "string").field("index", "not_analyzed").endObject()
                        .startObject("eventKind").field("type", "string").field("index", "not_analyzed").endObject()
                        .startObject("time").field("type", "date").endObject()
                        .startObject("start").field("type", "long").endObject()
                        .startObject("end").field("type", "long").endObject()
                    .endObject()
                .endObject()
            .endObject()
        .endObject()
    .endObject()

    val stat = XContentFactory.jsonBuilder()
    .startObject()
        .startObject("stat")
            .startObject("properties")
                .startObject("path").field("type", "string").field("index", "not_analyzed").endObject()
                .startObject("time").field("type", "date").endObject()
                .startObject("stat")
                    .startObject("properties")
                        .startObject("median").field("type", "long").endObject()
                        .startObject("average").field("type", "long").endObject()
                        .startObject("average95").field("type", "long").endObject()
                        .startObject("average99").field("type", "long").endObject()
                        .startObject("max").field("type", "long").endObject()
                        .startObject("min").field("type", "long").endObject()
                        .startObject("count").field("type", "long").endObject()
                    .endObject()
                .endObject()
            .endObject()
        .endObject()
    .endObject()

    search.admin().indices().prepareCreate("metrics").addMapping("error", error).addMapping("profile", profile).addMapping("stat", stat).get()
}