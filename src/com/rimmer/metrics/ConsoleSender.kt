package com.rimmer.metrics

import org.joda.time.DateTime

/** A metrics sender that writes to stdout. */
class ConsoleSender: Sender {
    override fun sendProfile(path: String, time: DateTime, start: Long, end: Long, events: List<Event>) {
        println("Profile of path $path at $time:")
        println("   start at $start")
        for(e in events) {
            println("   event in ${e.event} (${e.type.take(50)}) at ${e.startTime} (duration ${(e.endTime - e.startTime) / 1000000.0} ms)")
        }

        println("   end at $end (total duration: ${(end - start) / 1000000.0} ms)")
    }

    override fun sendStatistic(path: String, time: DateTime, stat: Statistic) {
        println("Stats for path $path at $time over ${stat.count} calls:")
        println("   average time: ${stat.average / 1000000.0} ms")
        println("   median time: ${stat.median / 1000000.0} ms")
        println("   95th percentile: ${stat.average95 / 1000000.0} ms")
        println("   99th percentile: ${stat.average99 / 1000000.0} ms")
        println("   min time: ${stat.min / 1000000.0} ms")
        println("   max time: ${stat.max / 1000000.0} ms")
    }

    override fun sendError(path: String, time: DateTime, reason: String) {
        println("An error occurred in the path $path at $time:")
        println(reason)
    }
}