package com.rimmer.metrics.server

import com.rimmer.metrics.generated.type.*
import com.rimmer.metrics.generated.type.Event
import com.rimmer.metrics.server.generated.type.*
import org.joda.time.DateTime
import java.util.*

val Interval.length: Long get() = end - start

class ErrorInstance(val time: DateTime, val trace: String, val cause: String, val parameters: Map<String, String>)

class ErrorClass(val cause: String, val trace: String) {
    var count = 0
    var lastOccurrence = DateTime.now()
    var instances = ArrayList<ErrorInstance>()
}

open class TimeSlice(val time: DateTime)

class PathMetric {
    var totalTime = 0L
    var totalCalls = 0
    var median = 0L
    var median95 = 0L
    var median99 = 0L
    var min = 0L
    var max = 0L

    // Sorted by interval length.
    // Used to incrementally build data points for this minute until it is closed.
    // This is cleared when more than a minute has elapsed.
    var statsBuilder: List<Interval> = emptyList()
}

class Metric(time: DateTime): TimeSlice(time) {
    var totalTime = 0L
    var totalCalls = 0
    var median = 0L
    var median95 = 0L
    var median99 = 0L
    var min = 0L
    var max = 0L

    val paths = HashMap<String, PathMetric>()

    // Sorted by interval length.
    // Used to incrementally build data points for this minute until it is closed.
    // This is cleared when more than a minute has elapsed.
    var statsBuilder: List<Interval> = emptyList()
}

class ProfilePoint(val server: String, val startTime: Long, val endTime: Long, val events: List<Event>)

class PathProfile {
    var normal: ProfilePoint? = null
    var max: ProfilePoint? = null
    val profileBuilder = ArrayList<ProfilePoint>()
}

class Profile(time: DateTime): TimeSlice(time) {
    val paths = HashMap<String, PathProfile>()
}

fun statEntry(it: Metric) = StatEntry(
    it.median.toFloat(),
    it.totalTime.toFloat() / it.totalCalls.toFloat(),
    it.median95.toFloat(),
    it.median99.toFloat(),
    it.max.toFloat(),
    it.min.toFloat(),
    it.totalCalls
)

fun statEntry(it: PathMetric) = StatEntry(
    it.median.toFloat(),
    it.totalTime.toFloat() / it.totalCalls.toFloat(),
    it.median95.toFloat(),
    it.median99.toFloat(),
    it.max.toFloat(),
    it.min.toFloat(),
    it.totalCalls
)

fun profileEntry(it: ProfilePoint?) = ProfileEntry(it?.startTime ?: 0, it?.endTime ?: 0, it?.events?.map {
    com.rimmer.metrics.server.generated.type.Event(it.event.name, it.type, it.startTime, it.endTime)
} ?: emptyList())

fun profileStat(it: PathProfile) = ProfileStat(profileEntry(it.normal), profileEntry(it.max))

class MetricStore {
    val inFlightTimes = ArrayList<Metric>()
    val inFlightProfiles = ArrayList<Profile>()

    val timeMap = HashMap<Long, Metric>()
    val profileMap = HashMap<Long, Profile>()
    val errorMap = HashMap<String, HashMap<String, ErrorClass>>()

    @Synchronized fun getStats(from: Long, to: Long): StatsPacket {
        if(inFlightTimes.isEmpty()) {
            return StatsPacket(emptyList())
        }

        val first = if(inFlightTimes.first().time.millis >= from) {
            0
        } else {
            val index = inFlightTimes.binarySearch {it.time.millis.compareTo(from)}
            if(index > 0) index else -index
        }

        val last = if(inFlightTimes.last().time.millis < to) {
            inFlightTimes.size
        } else {
            val index = inFlightTimes.binarySearch {it.time.millis.compareTo(to)}
            if(index > 0) index + 1 else -index
        }

        val list = inFlightTimes.subList(first, last)
        println("Returning stats with ${list.size} slices.")

        return StatsPacket(list.map {
            StatSlice(it.time, statEntry(it), it.paths.mapValues {statEntry(it.value)})
        })
    }

    @Synchronized fun getProfiles(from: Long, to: Long): ProfilesPacket {
        if(inFlightProfiles.isEmpty()) {
            return ProfilesPacket(emptyList())
        }

        val first = if(inFlightProfiles.first().time.millis >= from) {
            0
        } else {
            val index = inFlightProfiles.binarySearch {it.time.millis.compareTo(from)}
            if(index > 0) index else -index
        }

        val last = if(inFlightProfiles.last().time.millis < to) {
            inFlightProfiles.size
        } else {
            val index = inFlightProfiles.binarySearch {it.time.millis.compareTo(to)}
            if(index > 0) index + 1 else -index
        }

        val list = inFlightProfiles.subList(first, last)
        println("Returning stats with ${list.size} slices.")

        return ProfilesPacket(list.map {
            ProfileSlice(it.time, it.paths.mapValues {profileStat(it.value)})
        })
    }

    @Synchronized fun onStat(packet: StatPacket) {
        println("Received stat packet with ${packet.intervals.size} intervals.")

        // Remove the stats builder from any old in-flight points.
        removeOldPoints(packet.time, inFlightTimes)

        val key = packet.time.millis / 60000
        val existing = timeMap[key]
        val point = if(existing === null) {
            // Remove older points until we have at most one day of metrics left.
            if(inFlightTimes.size > 24*60) {
                inFlightTimes.subList(0, inFlightTimes.size - 24*60).clear()
            }

            val p = Metric(DateTime(key * 60000))
            timeMap.put(key, p)
            inFlightTimes.add(p)
            p
        } else existing

        val existingPath = point.paths[packet.path]
        val path = if(existingPath === null) {
            val p = PathMetric()
            point.paths[packet.path] = p
            p
        } else existingPath

        point.totalCalls += packet.intervals.size
        point.totalTime += packet.totalElapsed
        path.totalCalls += packet.intervals.size
        path.totalTime += packet.totalElapsed

        val stats = mergeIntervals(point.statsBuilder, packet.intervals)
        if(stats.isNotEmpty()) {
            point.max = stats[stats.size - 1].length
            point.min = stats[0].length
            point.median = stats[stats.size / 2].length
            point.median95 = stats[Math.ceil((stats.size - 1).toDouble() * 0.95).toInt()].length
            point.median99 = stats[Math.ceil((stats.size - 1).toDouble() * 0.99).toInt()].length
            point.statsBuilder = stats
        }

        val pathStats = mergeIntervals(path.statsBuilder, packet.intervals)
        if(pathStats.isNotEmpty()) {
            path.max = pathStats[pathStats.size - 1].length
            path.min = pathStats[0].length
            path.median = pathStats[pathStats.size / 2].length
            path.median95 = pathStats[Math.ceil((pathStats.size - 1).toDouble() * 0.95).toInt()].length
            path.median99 = pathStats[Math.ceil((pathStats.size - 1).toDouble() * 0.99).toInt()].length
            path.statsBuilder = pathStats
        }
    }

    @Synchronized fun onProfile(packet: ProfilePacket) {
        println("Received profile packet with ${packet.events.size} events.")

        // Remove the profile builder from any old in-flight profiles.
        removeOldProfiles(packet.time, inFlightProfiles)

        val key = packet.time.millis / 60000
        val existing = profileMap[key]
        val profile = if(existing === null) {
            // Remove older points until we have at most one day of metrics left.
            if(inFlightProfiles.size > 24*60) {
                inFlightProfiles.subList(0, inFlightProfiles.size - 24*60).clear()
            }

            val p = Profile(DateTime(key * 60000))
            profileMap.put(key, p)
            inFlightProfiles.add(p)
            p
        } else existing

        val existingPath = profile.paths[packet.path]
        val path = if(existingPath === null) {
            val p = PathProfile()
            profile.paths[packet.path] = p
            p
        } else existingPath

        // Keep the profile list sorted by duration.
        val duration = packet.end - packet.start
        var insertIndex = 0
        path.profileBuilder.find {insertIndex++; (it.endTime - it.startTime) > duration}
        path.profileBuilder.add(insertIndex - 1, ProfilePoint(packet.server, packet.start, packet.end, packet.events))

        path.normal = path.profileBuilder[path.profileBuilder.size / 2]
        path.max = path.profileBuilder[path.profileBuilder.size - 1]
    }

    @Synchronized fun onError(packet: ErrorPacket) {
        println("Received error packet.")

        val classes = errorMap.getOrPut(packet.path) { HashMap<String, ErrorClass>() }
        val type = classes.getOrPut(packet.cause) {ErrorClass(packet.cause, packet.trace)}
        type.count++
        type.lastOccurrence = packet.time
        type.instances.add(ErrorInstance(packet.time, packet.trace, packet.cause, emptyMap()))
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

fun isOldPoint(time: DateTime, p: TimeSlice) = time.millis - p.time.millis > 60000

fun removeOldPoints(time: DateTime, points: MutableList<Metric>) {
    if(points.size < 2) return

    var i = points.size - 1
    while(i > 0 && !isOldPoint(time, points[i])) {
        i--
    }
    val list = points.subList(0, i)
    list.forEach {
        it.statsBuilder = emptyList()
        it.paths.forEach { p, path ->
            path.statsBuilder = emptyList()
        }
    }
}

fun removeOldProfiles(time: DateTime, profiles: MutableList<Profile>) {
    if(profiles.size < 2) return

    var i = profiles.size - 1
    while(i > 0 && !isOldPoint(time, profiles[i])) {
        i--
    }
    val list = profiles.subList(0, i)
    list.forEach {
        it.paths.forEach { p, path ->
            path.profileBuilder.clear()
            path.profileBuilder.trimToSize()
        }
    }
}