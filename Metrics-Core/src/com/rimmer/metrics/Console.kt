package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*

/** A metrics sender that writes to stdout. */
class ConsoleSender: (MetricPacket) -> Unit {
    override fun invoke(metric: MetricPacket) {
        when(metric) {
            is ProfilePacket -> {
                println("Profile of path ${metric.location} at ${metric.time}:")
                println("   start at ${metric.start}")
                for ((type, description, startTime, endTime) in metric.events) {
                    println("   $startTime [${(endTime - startTime) / 1000000.0} ms] $type")
                    println("       $description")
                }

                println("   end at ${metric.end} (total duration: ${(metric.end - metric.start) / 1000000.0} ms)")
            }
            is StatPacket -> {
                println("Stats for path ${metric.location} at ${metric.time} over ${metric.sampleCount} calls:")
                println("   average time: ${metric.total / metric.sampleCount / 1000000.0} ms")
                println("   median time: ${metric.median / 1000000.0} ms")
                println("   95th percentile: ${metric.average95 / 1000000.0} ms")
                println("   99th percentile: ${metric.average99 / 1000000.0} ms")
                println("   min time: ${metric.min / 1000000.0} ms")
                println("   max time: ${metric.max / 1000000.0} ms")
            }
            is ErrorPacket -> {
                println("An error occurred in the path ${metric.location} at ${metric.time}:")
                println(metric.cause)
            }
        }
    }
}