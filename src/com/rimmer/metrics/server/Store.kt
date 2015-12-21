package com.rimmer.metrics.server

import org.joda.time.DateTime
import java.util.*

class ErrorInstance(val time: DateTime, val trace: String, val parameters: Map<String, String>)

class ErrorClass(val cause: String, val trace: String) {
    var count = 0
    var lastOccurrence = DateTime.now()
    var instances = ArrayList<ErrorInstance>()
}

class Metric(val path: String, val time: DateTime) {
    var totalTime = 0L
    var totalCalls = 0L
    var median = 0L
    var median95 = 0L
    var median99 = 0L
    var min = 0L
    var max = 0L

    // Sorted by interval length.
    // Used to iteratively build data points for this minute until it is closed.
    // This is cleared when more than a minute has elapsed.
    var statsBuilder: List<Interval> = emptyList()
}

class ProfilePoint(val server: String, val startTime: Long, val endTime: Long, val events: List<Event>)

class Profile(val path: String, val time: DateTime) {
    var normal: ProfilePoint? = null
    var max: ProfilePoint? = null
    val profileBuilder = ArrayList<ProfilePoint>()
}

class MetricStore {
    val inFlightTimes = ArrayList<Metric>()
    val inFlightProfiles = ArrayList<Profile>()

    val timeMap = HashMap<Long, Metric>()
    val profileMap = HashMap<Long, Profile>()
    val errorMap = HashMap<String, HashMap<String, ErrorClass>>()

    @Synchronized fun onStat(packet: StatPacket) {
        // Remove the stats builder from any old in-flight points.
        removeOldPoints(packet.time, inFlightTimes)

        val key = packet.time.millis / 60000
        val existing = timeMap[key]
        val point = if(existing === null) {
            val p = Metric(packet.path, DateTime(key * 60000))
            timeMap.put(key, p)
            inFlightTimes.add(p)
            p
        } else existing

        point.totalCalls += packet.intervals.size
        point.totalTime += packet.totalElapsed

        val stats = mergeIntervals(point.statsBuilder, packet.intervals)
        if(stats.isNotEmpty()) {
            point.max = stats[stats.size - 1].length
            point.min = stats[0].length
            point.median = stats[stats.size / 2].length
            point.median95 = stats[Math.ceil((stats.size - 1).toDouble() * 0.95).toInt()].length
            point.median99 = stats[Math.ceil((stats.size - 1).toDouble() * 0.99).toInt()].length
            point.statsBuilder = stats
        }
    }

    @Synchronized fun onProfile(packet: ProfilePacket) {
        // Remove the profile builder from any old in-flight profiles.
        removeOldProfiles(packet.time, inFlightProfiles)

        val key = packet.time.millis / 60000
        val existing = profileMap[key]
        val profile = if(existing === null) {
            val p = Profile(packet.path, DateTime(key * 60000))
            profileMap.put(key, p)
            inFlightProfiles.add(p)
            p
        } else existing

        // Keep the profile list sorted by duration.
        val duration = packet.end - packet.start
        var insertIndex = 0
        profile.profileBuilder.find {insertIndex++; (it.endTime - it.startTime) > duration}
        profile.profileBuilder.add(insertIndex - 1, ProfilePoint(packet.server, packet.start, packet.end, packet.events))

        profile.normal = profile.profileBuilder[profile.profileBuilder.size / 2]
        profile.max = profile.profileBuilder[profile.profileBuilder.size - 1]
    }

    @Synchronized fun onError(packet: ErrorPacket) {
        val classes = errorMap.getOrPut(packet.path) {HashMap<String, ErrorClass>()}
        val type = classes.getOrPut(packet.cause) {ErrorClass(packet.cause, packet.trace)}
        type.count++
        type.lastOccurrence = packet.time
        type.instances.add(ErrorInstance(packet.time, packet.trace, packet.parameters))
    }
}

fun mergeIntervals(first: List<Interval>, second: List<Interval>): List<Interval> {
    val target = ArrayList<Interval>(first.size + second.size)
    var f = 0
    var s = 0
    while(f < first.size && s < second.size) {
        if(first[f].length < second[s].length) {
            target.add(first[f])
            f++
        } else {
            target.add(second[s])
            s++
        }
    }

    while(f < first.size) {
        target.add(first[f])
        f++
    }

    while(s < second.size) {
        target.add(second[s])
        s++
    }
    return target
}

fun isOldPoint(time: DateTime, p: Metric) = time.millis - p.time.millis > 60000

fun removeOldPoints(time: DateTime, points: MutableList<Metric>) {
    var i = 0
    while(i < points.size && isOldPoint(time, points[i])) {
        i++
    }
    val list = points.subList(0, i)
    list.forEach {it.statsBuilder = emptyList()}
    list.clear()
}

fun isOldProfile(time: DateTime, p: Profile) = time.millis - p.time.millis > 60000

fun removeOldProfiles(time: DateTime, profiles: MutableList<Profile>) {
    var i = 0
    while(i < profiles.size && isOldProfile(time, profiles[i])) {
        i++
    }
    val list = profiles.subList(0, i)
    list.forEach {it.profileBuilder.clear(); it.profileBuilder.trimToSize()}
    list.clear()
}