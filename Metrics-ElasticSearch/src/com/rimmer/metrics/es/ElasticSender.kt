package com.rimmer.metrics.es

import com.rimmer.metrics.*
import com.rimmer.metrics.generated.type.*
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentFactory

/** A metrics sender implementation for ElasticSearch. */
class ElasticSender(val search: Client): Sender {
    override fun sendError(error: ErrorPacket) {
        search.prepareIndex("metrics", "error").setSource(
            XContentFactory.jsonBuilder().startObject()
                .field("path", error.path)
                .field("time", error.time)
                .field("reason", error.cause)
            .endObject()
        ).execute()
    }

    override fun sendProfile(profile: ProfilePacket) {
        val json = XContentFactory.jsonBuilder()
        json.startObject().field("path", profile.path).field("time", profile.time)
        json.startArray("events")
        profile.events.forEach {
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

    override fun sendStatistic(stat: StatPacket) {
        val json = XContentFactory.jsonBuilder()
        .startObject()
            .field("path", stat.path)
            .field("time", stat.time)
            .startObject("stat")
                .field("median", percentile(stat.intervals, 0.5))
                .field("average", stat.totalElapsed / stat.intervals.size)
                .field("average95", percentile(stat.intervals, 0.95))
                .field("average99", percentile(stat.intervals, 0.99))
                .field("max", percentile(stat.intervals, 1.0))
                .field("min", percentile(stat.intervals, 0.0))
                .field("count", stat.intervals.size)
            .endObject()
        .endObject()

        search.prepareIndex("metrics", "stat").setSource(json).execute()
    }

    fun percentile(intervals: List<Interval>, percentile: Double): Long {
        val e = intervals[Math.ceil(intervals.size * percentile).toInt()]
        return e.end - e.start
    }
}

/** Call this to recreate the index. This removes any existing metrics data! */
fun createElasticIndex(search: Client) {
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