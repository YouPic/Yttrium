package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*

/** A metrics sender that writes to stdout. */
class ConsoleSender: Sender {
    override fun sendProfile(profile: ProfilePacket) {
        println("Profile of path ${profile.path} at ${profile.time}:")
        println("   start at ${profile.start}")
        for(e in profile.events) {
            println("   event in ${e.event} (${e.type.take(50)}) at ${e.startTime} (duration ${(e.endTime - e.startTime) / 1000000.0} ms)")
        }

        println("   end at ${profile.end} (total duration: ${(profile.end - profile.start) / 1000000.0} ms)")
    }

    override fun sendStatistic(stat: StatPacket) {
        println("Stats for path ${stat.path} at ${stat.time} over ${stat.intervals.size} calls:")
        if(stat.intervals.isNotEmpty()) {
            println("   average time: ${stat.totalElapsed / stat.intervals.size / 1000000.0} ms")
            println("   median time: ${percentile(stat.intervals, 0.5) / 1000000.0} ms")
            println("   95th percentile: ${percentile(stat.intervals, 0.95) / 1000000.0} ms")
            println("   99th percentile: ${percentile(stat.intervals, 0.99) / 1000000.0} ms")
            println("   min time: ${percentile(stat.intervals, 0.0) / 1000000.0} ms")
            println("   max time: ${percentile(stat.intervals, 1.0) / 1000000.0} ms")
        }
    }

    override fun sendError(error: ErrorPacket) {
        println("An error occurred in the path ${error.path} at ${error.time}:")
        println(error.cause)
    }

    fun percentile(intervals: List<Interval>, percentile: Double): Long {
        val e = intervals[Math.ceil((intervals.size - 1) * percentile).toInt()]
        return e.end - e.start
    }
}