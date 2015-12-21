package com.rimmer.metrics.server

import com.rimmer.metrics.metrics.EventType
import org.joda.time.DateTime

val statsAggregator = "metrics.aggregator.stat"
val errorAggregator = "metrics.aggregator.error"
val profileAggregator = "metrics.aggregator.profile"

class Interval(val start: Long, val end: Long) {
    val length: Long get() = end - start
}

/** The interval list must be sorted from short duration to long duration. */
class StatPacket(val path: String, val server: String, val time: DateTime, val totalElapsed: Long, val intervals: List<Interval>)

class ErrorPacket(val path: String, val time: DateTime, val cause: String, val trace: String, val parameters: Map<String, String>)

data class Event(val event: EventType, val type: String, val startDate: DateTime, val startTime: Long, val endTime: Long)

class ProfilePacket(val path: String, val server: String, val time: DateTime, val start: Long, val end: Long, val events: List<Event>)