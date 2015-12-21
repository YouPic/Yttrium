
package com.rimmer.metrics

import com.rimmer.metrics.server.*
import org.joda.time.DateTime
import java.util.*

enum class EventType {redis, mysql, mysql_generate, mysql_process, mongo, serialize}

interface MetricsWriter {
    /**
     * Starts a new profiler event within the current call.
     * @return An event index to send to endQuery.
     */
    fun startEvent(eventType: EventType, type: String): Int

    /** Indicates that the provided event id has finished. */
    fun endEvent(id: Int)
}

interface Sender {
    fun sendStatistic(stat: StatPacket)
    fun sendProfile(profile: ProfilePacket)
    fun sendError(error: ErrorPacket)
}

class Metrics: MetricsWriter {
    inner class Call(val path: String, val startDate: DateTime, val startTime: Long, val parameters: Map<String, String>) {
        val events = ArrayList<Event>()
        var endTime = startTime
        var failed = false
        var error = false
        var failReason = ""
        var failTrace = ""
    }

    inner class Path {
        var lastSend = System.nanoTime()
        val calls = ArrayList<Call>()
    }

    var sender: Sender? = null
    private val paths = HashMap<String, Path>()
    private val currentCall = ThreadLocal<Call>()

    /** Indicates the start of a new action. All timed actions performed from this thread are added to the action. */
    fun start(path: String, parameters: Map<String, String>) {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        currentCall.set(Call(path, startDate, startTime, parameters))
    }

    /** Indicates that the current action has completed. It is added to the metrics. */
    fun finish() {
        val time = System.nanoTime()
        val call = currentCall.get()
        call.endTime = time

        val path = synchronized(this) {
            var path = paths[call.path]
            if(path === null) {
                path = Path()
                paths.put(call.path, path)
            }
            path
        }!!

        synchronized(path) {
            path.calls.add(call)
            if(time - path.lastSend > 60000000000L) {
                sendStats(path)
                path.calls.clear()
                path.lastSend = time
            }
        }
    }

    /** Discards the current action without adding to the metrics, for example if the call was forwarded. */
    fun discard() { currentCall.set(null) }

    /**
     * Indicates that the current call did not complete correctly.
     * Incorrect calls are not used in the metrics.
     * @param wasError If set, the failure is reported in the metrics as an internal server error.
     */
    fun fail(wasError: Boolean, reason: String) {
        currentCall.get()?.run {
            failed = true
            error = wasError
            failReason = reason
            finish()
        }
    }

    override fun startEvent(eventType: EventType, type: String): Int {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        return currentCall.get()?.run {
            events.add(Event(eventType, type, startDate, startTime, startTime))
            events.size - 1
        } ?: 0
    }

    override fun endEvent(id: Int) {
        currentCall.get()?.run {
            val event = events[id]
            events[id] = event.copy(endTime = System.nanoTime())
        }
    }

    private fun sendStats(path: Path) {
        // The sender may throw exceptions, which we don't want to leak.
        try {
            sender?.run {
                // Filter out any failed requests. If it was an internal failure we log it.
                val calls = ArrayList<Call>(path.calls.size)
                path.calls.filterTo(calls) {
                    if (it.error) {
                        sendError(ErrorPacket(it.path, it.startDate, it.failReason, it.failTrace, it.parameters))
                    }
                    !it.failed
                }

                // If all calls failed, we have nothing more to do.
                if (calls.isEmpty()) return

                val date = calls[0].startDate
                val count = calls.size

                // We need to sort the calls on their duration to find the median and percentiles.
                // In the same loop we calculate the average.
                var totalTime = 0L
                calls.sortBy {
                    val elapsed = it.endTime - it.startTime
                    totalTime += elapsed
                    elapsed
                }

                if(calls.size == 1) {
                    totalTime = calls[0].endTime - calls[0].startTime
                }

                val median = calls[count / 2]
                val min = calls[0]
                val max = calls[count - 1]

                // Send the timing intervals for calculating statistics.
                sendStatistic(StatPacket(min.path, "", date, totalTime, calls.map {Interval(it.startTime, it.endTime)}))

                // Send a full profile for the median and maximum values.
                sendProfile(ProfilePacket(median.path, "", median.startDate, median.startTime, median.endTime, median.events))
                sendProfile(ProfilePacket(max.path, "", max.startDate, max.startTime, max.endTime, max.events))
            }
        } catch(e: Throwable) {
            println("Could not send metrics:")
            e.printStackTrace()
        }
    }
}