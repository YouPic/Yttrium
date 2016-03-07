package com.rimmer.metrics

import org.joda.time.DateTime

interface Sender {
    fun sendStatistic(stat: StatPacket)
    fun sendProfile(profile: ProfilePacket)
    fun sendError(error: ErrorPacket)
}

enum class EventType {redis, mysql, mysql_generate, mysql_process, mongo, serialize}

class Interval(val start: Long, val end: Long) {
    val length: Long get() = end - start
}

/** The interval list must be sorted from short duration to long duration. */
class StatPacket(val path: String, val server: String, val time: DateTime, val totalElapsed: Long, val intervals: List<Interval>)

class ErrorPacket(val path: String, val time: DateTime, val cause: String, val trace: String, val parameters: Map<String, String>)

data class Event(val event: EventType, val type: String, val startDate: DateTime, val startTime: Long, val endTime: Long)

class ProfilePacket(val path: String, val server: String, val time: DateTime, val start: Long, val end: Long, val events: List<Event>)