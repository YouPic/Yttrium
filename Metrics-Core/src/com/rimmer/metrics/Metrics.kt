package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*
import com.rimmer.yttrium.getOrAdd
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.concurrent.getOrSet

interface MetricsWriter {
    /**
     * Starts a new profiler event within the current call.
     * @return An event index to send to endQuery.
     */
    fun startEvent(call: Int, type: String, description: String): Int

    /** Indicates that the provided event id has finished. */
    fun endEvent(call: Int, id: Int)

    /** Adds a completed event, including the elapsed time. */
    fun onEvent(call: Int, type: String, description: String, elapsedNanos: Long)
}

/**
 * Handles metric events on a client.
 * All public functions are thread-safe.
 */
class Metrics(maxStats: Int = 32): MetricsWriter {
    enum class Accumulator { set, count, min, max }

    class Call(val path: String, val startDate: DateTime, val startTime: Long) {
        val events = ArrayList<Event>()
        var endTime = startTime
        var failed = false
        var error = false
        var failReason = ""
        var failTrace = ""
    }

    class CallData {
        val paths = HashMap<String, Path>()
        val calls = ArrayList<Call?>()
        var nextCall = 0
    }

    inner class Path {
        var lastSend = 0L
        var counter = 0
        var calls = ArrayList<Call>()
    }

    var sender: Sender? = null
    private val callThreads = ThreadLocal<CallData>()

    private val statNames = arrayOfNulls<String>(maxStats)
    private val statTypes = ByteArray(maxStats)
    private val statsValues = AtomicLongArray(maxStats)
    private val statChanges = BooleanArray(maxStats)
    private var statCount = 0

    /**
     * Registers a new statistic with the provided accumulator type.
     * @return An id that can be used for later calls concerning this statistic.
     */
    @Synchronized fun registerStat(category: String, type: Accumulator): Int {
        val currentIndex = statNames.indexOf(category)
        if(currentIndex < 0) {
            val index = statCount++
            if(index >= statNames.size) throw IllegalStateException("Too many statistic types.")

            statNames[index] = category
            statTypes[index] = type.ordinal.toByte()
            return index
        } else if(statTypes[currentIndex] !== type.ordinal.toByte()) {
            throw IllegalStateException("Statistic $category already exists under a different type.")
        } else {
            return currentIndex
        }
    }

    /** Updates the numeric statistic for the provided id. */
    fun setStat(id: Int, value: Long) {
        statChanges[id] = true
        when(statTypes[id]) {
            Accumulator.set.ordinal.toByte() -> statsValues.set(id, value)
            Accumulator.count.ordinal.toByte() -> statsValues.addAndGet(id, value)
            Accumulator.min.ordinal.toByte() -> {
                var v = value
                while(v < statsValues.get(id)) {
                    v = statsValues.getAndSet(id, v)
                }
            }
            Accumulator.max.ordinal.toByte() -> {
                var v = value
                while(v > statsValues.get(id)) {
                    v = statsValues.getAndSet(id, v)
                }
            }
        }
    }

    /** Logs a custom error packet. */
    fun logError(category: String, path: String, reason: String, description: String, trace: String, fatal: Boolean) {
        sender?.sendError(ErrorPacket(path, category, fatal, DateTime.now(), reason, description, trace, 1))
    }

    /** Indicates the start of a new action. All timed actions performed from this thread are added to the action. */
    fun start(path: String): Int {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        val call = Call(path, startDate, startTime)
        val thread = callThreads.getOrSet { CallData() }

        val i = thread.nextCall
        if(i >= thread.calls.size) {
            thread.calls.add(call)
            thread.nextCall++
        } else {
            thread.calls[i] = call
            val next = thread.calls.indexOfFirst { it == null }
            thread.nextCall = if(next == -1) thread.calls.size else next
        }
        return i
    }

    /** Indicates that the current action has completed. It is added to the metrics. */
    fun finish(callId: Int) {
        val time = System.nanoTime()
        val call = getCall(callId) ?: return
        val thread = callThreads.get() ?: return

        call.endTime = time

        var path = thread.paths[call.path]
        if(path === null) {
            path = Path()
            thread.paths.put(call.path, path)
        }

        val count = path.calls.size
        if(count < 20) {
            path.calls.add(call)
        } else {
            val stride = if(count < 100) 5 else if(count < 1000) 20 else 1000
            if(path.counter == 0) {
                path.calls.add(call)
            }
            path.counter++
            if(path.counter > stride) path.counter = 0
        }

        if((time - path.lastSend > 60000000000L) || (call.error && time - path.lastSend > 10000000000L)) {
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

    override fun startEvent(call: Int, type: String, description: String): Int {
        val startTime = System.nanoTime()
        return getCall(call)?.run {
            events.add(Event(type, description, startTime, startTime))
            events.size - 1
        } ?: 0
    }

    override fun endEvent(call: Int, id: Int) {
        getCall(call)?.run {
            val event = events[id]
            events[id] = event.copy(endTime = System.nanoTime())
        }
    }

    override fun onEvent(call: Int, type: String, description: String, elapsedNanos: Long) {
        getCall(call)?.run {
            val endTime = System.nanoTime()
            events.add(Event(type, description, endTime - elapsedNanos, endTime))
        }
    }

    private fun sendStats(path: Path) {
        // The sender may throw exceptions, which we don't want to leak.
        try {
            sender?.run {
                // Filter out any failed requests. If it was an internal failure we log it.
                val calls = ArrayList<Call>(path.calls.size)

                class Error(val start: DateTime, val path: String, val reason: String, val trace: String) {
                    var count = 0
                }
                val errors = HashMap<String, Error>()

                path.calls.filterTo(calls) {
                    if(it.error) {
                        // Avoid sending many errors of the same kind.
                        errors.getOrAdd(it.failReason) {
                            Error(it.startDate, it.path, it.failReason, it.failTrace)
                        }.count++
                    }
                    !it.failed
                }

                errors.forEach { s, error ->
                    sendError(ErrorPacket(
                        error.path, "", true, error.start, error.reason, "", error.trace, error.count
                    ))
                }

                // If all calls failed, we have nothing more to do.
                if(calls.isEmpty()) return

                val date = calls[0].startDate
                val count = calls.size

                // We need to sort the calls on their duration to find the median and percentiles.
                // In the same loop we calculate the average.
                calls.sortBy {
                    val elapsed = it.endTime - it.startTime
                    elapsed
                }

                var totalTime = 0L
                for(c in calls) {
                    totalTime += c.endTime - c.startTime
                }

                val median = calls[count / 2]
                val min = calls[0]
                val max = calls[count - 1]
                val average95 = calls[Math.floor(count * 0.95).toInt()]
                val average99 = calls[Math.floor(count * 0.99).toInt()]

                // Send the timing intervals for calculating statistics.
                sendStatistic(StatPacket(
                    min.path, "", date, calls.size, totalTime,
                    min.endTime - min.startTime,
                    max.endTime - max.startTime,
                    median.endTime - median.startTime,
                    average95.endTime - average95.startTime,
                    average99.endTime - average99.startTime
                ))

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
        val calls = callThreads.get()?.calls ?: return null
        if(calls.size <= id || calls[id] == null) {
            println("Received unknown call id $id")
            return null
        } else {
            return calls[id]
        }
    }

    private fun removeCall(id: Int) {
        val thread = callThreads.get() ?: return
        thread.calls[id] = null
        thread.nextCall = id
    }
}