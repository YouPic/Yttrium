package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*
import org.joda.time.DateTime
import java.util.*

interface MetricsWriter {
    /**
     * Starts a new profiler event within the current call.
     * @return An event index to send to endQuery.
     */
    fun startEvent(call: Int, eventType: EventType, type: String): Int

    /** Indicates that the provided event id has finished. */
    fun endEvent(call: Int, id: Int)

    /** Adds a completed event, including the elapsed time. */
    fun onEvent(call: Int, eventType: EventType, type: String, elapsedNanos: Long)
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
    private val calls = ArrayList<Call?>()
    private var nextCall = 0

    /** Indicates the start of a new action. All timed actions performed from this thread are added to the action. */
    fun start(path: String, parameters: Map<String, String>): Int {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        val call = Call(path, startDate, startTime, parameters)

        val i = nextCall
        if(i >= calls.size) {
            calls.add(call)
            nextCall++
        } else {
            calls[i] = call
            val next = calls.indexOfFirst { it == null }
            nextCall = if(next == -1) calls.size else next
        }
        return i
    }

    /** Indicates that the current action has completed. It is added to the metrics. */
    fun finish(callId: Int) {
        val time = System.nanoTime()
        val call = getCall(callId) ?: return
        call.endTime = time

        var path = paths[call.path]
        if(path === null) {
            path = Path()
            paths.put(call.path, path)
        }

        path.calls.add(call)
        if(time - path.lastSend > 60000000000L) {
            sendStats(path)
            path.calls.clear()
            path.lastSend = time
        }

        removeCall(callId)
    }

    /** Discards the current action without adding to the metrics, for example if the call was forwarded. */
    fun discard(call: Int) {
        removeCall(call)
    }

    /**
     * Indicates that the current call did not complete correctly.
     * Incorrect calls are not used in the metrics.
     * @param wasError If set, the failure is reported in the metrics as an internal server error.
     */
    fun fail(call: Int, wasError: Boolean, reason: String) {
        getCall(call)?.run {
            failed = true
            error = wasError
            failReason = reason
            finish(call)
        }
    }

    override fun startEvent(call: Int, eventType: EventType, type: String): Int {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        return getCall(call)?.run {
            events.add(Event(eventType, type, startDate, startTime, startTime))
            events.size - 1
        } ?: 0
    }

    override fun endEvent(call: Int, id: Int) {
        getCall(call)?.run {
            val event = events[id]
            events[id] = event.copy(endTime = System.nanoTime())
        }
    }

    override fun onEvent(call: Int, eventType: EventType, type: String, elapsedNanos: Long) {
        getCall(call)?.run {
            val startDate = DateTime.now().minusMillis((elapsedNanos / 1000000L).toInt())
            val endTime = System.nanoTime()
            events.add(Event(eventType, type, startDate, endTime - elapsedNanos, endTime))
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
                        sendError(ErrorPacket(it.path, it.startDate, it.failReason, it.failTrace))
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
                sendStatistic(StatPacket(min.path, "", date, totalTime, calls.map { Interval(it.startTime, it.endTime) }.toCollection(ArrayList())))

                // Send a full profile for the median and maximum values.
                sendProfile(ProfilePacket(median.path, "", median.startDate, median.startTime, median.endTime, median.events))
                sendProfile(ProfilePacket(max.path, "", max.startDate, max.startTime, max.endTime, max.events))
            }
        } catch(e: Throwable) {
            println("Could not send metrics:")
            e.printStackTrace()
        }
    }

    private fun getCall(id: Int): Call? {
        if(calls.size <= id || calls[id] == null) {
            println("Received unknown call id $id")
            return null
        } else {
            return calls[id]
        }
    }

    private fun removeCall(id: Int) {
        calls[id] = null
        nextCall = id
    }
}