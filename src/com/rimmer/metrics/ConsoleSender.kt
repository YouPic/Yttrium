package com.rimmer.metrics

import org.joda.time.DateTime

/** A metrics sender that writes to stdout. */
class ConsoleSender: Sender {
    override fun sendProfile(path: String, time: DateTime, start: Long, end: Long, events: List<Event>) {
        println("Profile of path $path at $time:")
        println("   start at $start")
        for(e in events) {
            println("   event in ${e.event} (${e.type.take(50)}) at ${e.startTime} (duration ${e.endTime - e.startTime})")
        }

        println("   end at $end (total duration: ${end - start})")
    }

    override fun sendStatistic(path: String, time: DateTime, stat: Statistic) {
        println("Stats for path $path at $time over ${stat.count} calls:")
        println("   average time: ${stat.average}")
        println("   median time: ${stat.median}")
        println("   95th percentile: ${stat.average95}")
        println("   99th percentile: ${stat.average99}")
        println("   min time: ${stat.min}")
        println("   max time: ${stat.max}")
    }

    override fun sendError(path: String, time: DateTime, reason: String) {
        println("An error occurred in the path $path at $time:")
        println(reason)
    }
}