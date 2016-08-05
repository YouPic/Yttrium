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
                    println("   $startTime [${decimals((endTime - startTime) / 1000000.0, 2)} ms] $type")
                    println("       $description")
                }

                println("   end at ${metric.end} (total duration: ${decimals((metric.end - metric.start) / 1000000.0, 2)} ms)")
            }
            is StatPacket -> {
                if(metric.location === null) {
                    val over = if(metric.sampleCount > 1) "over ${metric.sampleCount} calls" else ""
                    println("Stats for ${metric.category} at ${metric.time} $over")
                } else {
                    println("Stats for ${metric.category} (${metric.location}) at ${metric.time} over ${metric.sampleCount} calls:")
                }

                if(metric.sampleCount <= 1) {
                    println("   value: ${convertValue(metric.median, metric.unit)}")
                } else {
                    println("   average: ${convertValue(metric.total / metric.sampleCount, metric.unit)}")
                    println("   median: ${convertValue(metric.median, metric.unit)}")
                    println("   95th percentile: ${convertValue(metric.average95, metric.unit)}")
                    println("   99th percentile: ${convertValue(metric.average99, metric.unit)}")
                    println("   min: ${convertValue(metric.min, metric.unit)}")
                    println("   max: ${convertValue(metric.max, metric.unit)}")
                }
            }
            is ErrorPacket -> {
                println("An error occurred in the path ${metric.location} at ${metric.time}:")
                println(metric.cause)
            }
        }
    }

    private fun convertValue(value: Long, unit: MetricUnit): String {
        return when(unit) {
            MetricUnit.CountUnit -> value.toString()
            MetricUnit.TimeUnit -> "${decimals(value / 1000000.0, 2)} ms"
            MetricUnit.ByteUnit -> {
                if(value < 1024) {
                    "$value B"
                } else if(value < 1024 * 1024) {
                    "${decimals(value / 1024.0, 1)} KB"
                } else if(value < 1024 * 1024 * 1024) {
                    "${decimals(value / (1024.0 * 1024.0), 1)} MB"
                } else {
                    "${decimals(value / (1024.0 * 1024.0 * 1024.0), 1)} GB"
                }
            }
            MetricUnit.FractionUnit -> "${decimals(value / 10000.0, 1)}%"
        }
    }

    private fun decimals(v: Double, decimals: Int): Double {
        val mul = when(decimals) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            else -> 1000.0
        }
        return Math.round(v * mul) / mul
    }
}