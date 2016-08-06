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
                    println("   $startTime [${metricDecimals((endTime - startTime) / 1000000.0, 2)} ms] $type")
                    println("       $description")
                }

                println("   end at ${metric.end} (total duration: ${metricDecimals((metric.end - metric.start) / 1000000.0, 2)} ms)")
            }
            is StatPacket -> {
                if(metric.location === null) {
                    val over = if(metric.sampleCount > 1) "over ${metric.sampleCount} calls" else ""
                    println("Stats for ${metric.category} at ${metric.time} $over")
                } else {
                    println("Stats for ${metric.category} (${metric.location}) at ${metric.time} over ${metric.sampleCount} calls:")
                }

                if(metric.sampleCount <= 1) {
                    println("   value: ${formatMetric(metric.median, metric.unit)}")
                } else {
                    println("   average: ${formatMetric(metric.total / metric.sampleCount, metric.unit)}")
                    println("   median: ${formatMetric(metric.median, metric.unit)}")
                    println("   95th percentile: ${formatMetric(metric.average95, metric.unit)}")
                    println("   99th percentile: ${formatMetric(metric.average99, metric.unit)}")
                    println("   min: ${formatMetric(metric.min, metric.unit)}")
                    println("   max: ${formatMetric(metric.max, metric.unit)}")
                }
            }
            is ErrorPacket -> {
                println("An error occurred in the path ${metric.location} at ${metric.time}:")
                println(metric.cause)
            }
        }
    }
}