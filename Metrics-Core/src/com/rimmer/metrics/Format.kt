package com.rimmer.metrics

import com.rimmer.metrics.generated.type.MetricUnit

fun formatMetric(value: Long, unit: MetricUnit): String {
    return when(unit) {
        MetricUnit.CountUnit -> value.toString()
        MetricUnit.TimeUnit -> "${metricDecimals(value / 1000000.0, 2)} ms"
        MetricUnit.ByteUnit -> {
            when {
                value < 1024 -> "$value B"
                value < 1024 * 1024 -> "${metricDecimals(value / 1024.0, 1)} KB"
                value < 1024 * 1024 * 1024 -> "${metricDecimals(value / (1024.0 * 1024.0), 1)} MB"
                else -> "${metricDecimals(value / (1024.0 * 1024.0 * 1024.0), 1)} GB"
            }
        }
        MetricUnit.FractionUnit -> "${metricDecimals(value / 10000.0, 1)}%"
    }
}

fun metricDecimals(v: Double, decimals: Int): Double {
    val mul = when(decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        else -> 1000.0
    }
    return Math.round(v * mul) / mul
}