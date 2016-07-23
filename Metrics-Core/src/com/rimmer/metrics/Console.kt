package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*

/** A metrics sender that writes to stdout. */
class ConsoleSender: Sender {
    override fun sendProfile(profile: ProfilePacket) {
        println("Profile of path ${profile.location} at ${profile.time}:")
        println("   start at ${profile.start}")
        for ((type, description, startTime, endTime) in profile.events) {
            println("   $startTime [${(endTime - startTime) / 1000000.0} ms] $type")
            println("       $description")
        }

        println("   end at ${profile.end} (total duration: ${(profile.end - profile.start) / 1000000.0} ms)")
    }

    override fun sendStatistic(stat: StatPacket) {
        println("Stats for path ${stat.location} at ${stat.time} over ${stat.sampleCount} calls:")
        println("   average time: ${stat.total / stat.sampleCount / 1000000.0} ms")
        println("   median time: ${stat.median / 1000000.0} ms")
        println("   95th percentile: ${stat.average95 / 1000000.0} ms")
        println("   99th percentile: ${stat.average99 / 1000000.0} ms")
        println("   min time: ${stat.min / 1000000.0} ms")
        println("   max time: ${stat.max / 1000000.0} ms")
    }

    override fun sendError(error: ErrorPacket) {
        println("An error occurred in the path ${error.location} at ${error.time}:")
        println(error.cause)
    }
}