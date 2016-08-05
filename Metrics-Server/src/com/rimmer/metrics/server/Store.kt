package com.rimmer.metrics.server

import com.rimmer.metrics.generated.type.*
import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.server.generated.type.*
import com.rimmer.metrics.server.generated.type.TimeMetric as TimeMetricResponse
import com.rimmer.metrics.server.generated.type.ServerMetric as ServerMetricResponse
import com.rimmer.metrics.server.generated.type.CategoryMetric as CategoryMetricResponse
import com.rimmer.metrics.server.generated.type.Metric as MetricResponse
import com.rimmer.metrics.server.generated.type.TimeProfile as TimeProfileResponse
import com.rimmer.metrics.server.generated.type.CategoryProfile as CategoryProfileResponse
import com.rimmer.metrics.server.generated.type.ErrorClass as ErrorClassResponse
import com.rimmer.yttrium.getOrAdd
import org.joda.time.DateTime
import java.util.*

fun profileEntry(it: ProfileEntry?) = ProfileEntry(it?.start ?: 0, it?.end ?: 0, it?.events?.map {
    ProfileEvent(it.group, it.event, it.startTime, it.endTime)
} ?: emptyList())

fun profileStat(it: Profile) = ProfileStat(profileEntry(it.normal), profileEntry(it.max))

class ErrorClass(val cause: String, val trace: String, val fatal: Boolean) {
    var count = 0
    var lastOccurrence: DateTime = DateTime.now()
    var servers = HashMap<String, ArrayList<ErrorInstance>>()
}

open class TimeSlice(val time: DateTime)

class Metric {
    var totalTime = 0L
    var totalCalls = 0
    var median = 0L
    var median95 = 0L
    var median99 = 0L
    var min = 0L
    var max = 0L

    fun update(packet: StatPacket) {
        // We don't have enough information to calculate the exact values,
        // since that would require sending detailed information about every single call.
        // Instead, we calculate the medians through an approximation.
        median = (median * totalCalls + packet.median * packet.sampleCount) / (totalCalls + packet.sampleCount)
        median95 = (median95 * totalCalls + packet.average95 * packet.sampleCount) / (totalCalls + packet.sampleCount)
        median99 = (median99 * totalCalls + packet.average99 * packet.sampleCount) / (totalCalls + packet.sampleCount)

        // Update the other aggregates.
        totalCalls += packet.sampleCount
        totalTime += packet.total
        max = Math.max(packet.max, max)
        min = Math.min(packet.min, min)
    }

    fun toResponse() = MetricResponse(
        median.toFloat(), totalTime.toFloat() / totalCalls.toFloat(), median95.toFloat(),
        median99.toFloat(), max.toFloat(), min.toFloat(), totalCalls
    )
}

class CategoryMetric(val category: String, val unit: MetricUnit) {
    val metric = Metric()
    val paths = HashMap<String, Metric>()
}

class ServerMetric(val server: String) {
    val metric = Metric()
    val categories = HashMap<String, CategoryMetric>()
}

class TimeMetric(time: DateTime): TimeSlice(time) {
    val metric = Metric()
    val servers = HashMap<String, ServerMetric>()
}

class Profile {
    var normal: ProfileEntry? = null
    var max: ProfileEntry? = null
    val profileBuilder = ArrayList<ProfileEntry>()
}

class CategoryProfile(val category: String) {
    val profile = Profile()
    val paths = HashMap<String, Profile>()
}

class TimeProfile(time: DateTime): TimeSlice(time) {
    val servers = HashMap<String, HashMap<String, CategoryProfile>>()
}

class MetricStore {
    val inFlightTimes = ArrayList<TimeMetric>()
    val inFlightProfiles = ArrayList<TimeProfile>()

    val timeMap = HashMap<Long, TimeMetric>()
    val profileMap = HashMap<Long, TimeProfile>()
    val errorMap = HashMap<String, HashMap<String, ErrorClass>>()

    @Synchronized fun getStats(from: Long, to: Long): List<TimeMetricResponse> {
        if(inFlightTimes.isEmpty()) {
            return emptyList()
        }

        val first = if(inFlightTimes.first().time.millis >= from) {
            0
        } else {
            val index = inFlightTimes.binarySearch {it.time.millis.compareTo(from)}
            if(index > 0) index else -index - 1
        }

        val last = if(inFlightTimes.last().time.millis < to) {
            inFlightTimes.size
        } else {
            val index = inFlightTimes.binarySearch {it.time.millis.compareTo(to)}
            if(index > 0) index + 1 else -index
        }

        val list = inFlightTimes.subList(first, last)
        println("Returning stats with ${list.size} slices.")

        return list.map {
            TimeMetricResponse(it.time, it.metric.toResponse(), it.servers.mapValues {
                ServerMetricResponse(it.value.metric.toResponse(), it.value.categories.mapValues {
                    CategoryMetricResponse(it.value.metric.toResponse(), it.value.unit, it.value.paths.mapValues {
                        it.value.toResponse()
                    })
                })
            })
        }
    }

    @Synchronized fun getProfiles(from: Long, to: Long): List<TimeProfileResponse> {
        if(inFlightProfiles.isEmpty()) {
            return emptyList()
        }

        val first = if(inFlightProfiles.first().time.millis >= from) {
            0
        } else {
            val index = inFlightProfiles.binarySearch {it.time.millis.compareTo(from)}
            if(index > 0) index else -index - 1
        }

        val last = if(inFlightProfiles.last().time.millis < to) {
            inFlightProfiles.size
        } else {
            val index = inFlightProfiles.binarySearch {it.time.millis.compareTo(to)}
            if(index > 0) index + 1 else -index
        }

        val list = inFlightProfiles.subList(first, last)
        println("Returning stats with ${list.size} slices.")

        return list.map {
            TimeProfileResponse(it.time, it.servers.mapValues {
                it.value.mapValuesTo(HashMap()) {
                    CategoryProfileResponse(
                        profileStat(it.value.profile),
                        it.value.paths.mapValues { profileStat(it.value) }
                    )
                }
            })
        }
    }

    @Synchronized fun getErrors(from: Long): List<ErrorClassResponse> {
        val errors = errorMap.mapValues {it.value.filterValues {it.lastOccurrence.millis > from}}
        return errors.flatMap { it.value.values.map {
            ErrorClassResponse(it.cause, it.count, it.fatal, it.lastOccurrence, it.servers)
        } }
    }

    @Synchronized fun onStat(packet: StatPacket, remoteName: String, remote: String) {
        println("Received stat packet with ${packet.sampleCount} samples.")

        val key = packet.time.millis / 60000
        val timePoint = timeMap.getOrAdd(key) {
            // Remove older points until we have at most one week of metrics left.
            if(inFlightTimes.size > 7*24*60) {
                inFlightTimes.subList(0, inFlightTimes.size - 7*24*60).clear()
            }

            val p = TimeMetric(DateTime(key * 60000))
            inFlightTimes.add(p)
            p
        }

        val name = remoteName(remoteName, remote)
        val serverPoint = timePoint.servers.getOrAdd(name) { ServerMetric(name) }
        val categoryPoint = serverPoint.categories.getOrAdd(packet.category) { CategoryMetric(packet.category, packet.unit) }
        val pathPoint = packet.location?.let { categoryPoint.paths.getOrAdd(it) { Metric() } }

        timePoint.metric.update(packet)
        serverPoint.metric.update(packet)
        categoryPoint.metric.update(packet)
        pathPoint?.update(packet)
    }

    @Synchronized fun onProfile(packet: ProfilePacket, remoteName: String, remote: String) {
        println("Received profile packet with ${packet.events.size} events.")

        // Remove the profile builder from any old in-flight profiles.
        removeOldProfiles(packet.time, inFlightProfiles)

        val key = packet.time.millis / 60000
        val timeProfile = profileMap.getOrAdd(key) {
            // Remove older points until we have at most one day of metrics left.
            if(inFlightProfiles.size > 24*60) {
                inFlightProfiles.subList(0, inFlightProfiles.size - 24*60).clear()
            }

            val p = TimeProfile(DateTime(key * 60000))
            inFlightProfiles.add(p)
            p
        }

        val name = remoteName(remoteName, remote)
        val serverProfile = timeProfile.servers.getOrAdd(name) { HashMap() }
        val categoryProfile = serverProfile.getOrAdd(packet.category) { CategoryProfile(packet.category) }
        val pathProfile = packet.location?.let { categoryProfile.paths.getOrAdd(it) { Profile() } }
        val profile = pathProfile ?: categoryProfile.profile

        // Keep the profile list sorted by duration.
        val duration = packet.end - packet.start
        var insertIndex = 0
        profile.profileBuilder.find {insertIndex++; (it.end - it.start) > duration}
        profile.profileBuilder.add(insertIndex - 1, ProfileEntry(packet.start, packet.end, packet.events.map {
            ProfileEvent(it.type, it.description, it.startTime, it.endTime)
        }))

        profile.normal = profile.profileBuilder[profile.profileBuilder.size / 2]
        profile.max = profile.profileBuilder.last()
    }

    @Synchronized fun onError(packet: ErrorPacket, remoteName: String, remote: String) {
        println("Received error packet.")

        val name = remoteName(remoteName, remote)
        val category = errorMap.getOrPut(packet.category) { HashMap() }
        val errorClass = category.getOrPut(packet.cause) { ErrorClass(packet.cause, packet.trace, packet.fatal) }
        errorClass.count++
        errorClass.lastOccurrence = packet.time
        errorClass.servers.getOrAdd(name) {
            ArrayList()
        }.add(ErrorInstance(packet.time, packet.trace, packet.location))
    }

    private fun remoteName(name: String, remote: String) = "$name [$remote]"
}

fun isOldPoint(time: DateTime, p: TimeSlice) = time.millis - p.time.millis > 60000

fun removeOldProfiles(time: DateTime, profiles: MutableList<TimeProfile>) {
    if(profiles.size < 2) return

    var i = profiles.size - 1
    while(i > 0 && !isOldPoint(time, profiles[i])) {
        i--
    }
    val list = profiles.subList(0, i)
    list.forEach {
        it.servers.forEach { s, server ->
            server.forEach { s, category ->
                category.profile.profileBuilder.clear()
                category.profile.profileBuilder.trimToSize()
                category.paths.forEach { p, path ->
                    path.profileBuilder.clear()
                    path.profileBuilder.trimToSize()
                }
            }
        }
    }
}