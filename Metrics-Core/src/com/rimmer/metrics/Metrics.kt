package com.rimmer.metrics

import com.rimmer.metrics.generated.type.*
import com.rimmer.yttrium.getOrAdd
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.concurrent.getOrSet

interface MetricsWriter {
    /**
     * Starts a new profiler event within the current call.
     * @return An event index to send to endQuery.
     */
    fun startEvent(call: Any?, type: String, description: String): Int

    /** Indicates that the provided event id has finished. */
    fun endEvent(call: Any?, id: Int)

    /** Adds a completed event, including the elapsed time. */
    fun onEvent(call: Any?, type: String, description: String, elapsedNanos: Long)
}

/**
 * Handles metric events on a client.
 * All public functions are thread-safe.
 */
class Metrics(maxStats: Int = 64): MetricsWriter {
    enum class Accumulator { set, count, min, max }
    enum class Scope { local, global }

    class Call(
        val path: Path, val category: String,
        val startDate: DateTime, val startTime: Long,
        val nextData: Any?
    ) {
        val events = ArrayList<Event>()
        var endTime = 0L
        var failed = false
    }

    class Error(
        val start: DateTime, val path: Path, val description: String,
        val reason: String, val trace: String, val fatal: Boolean
    ) {
        var count = 1
    }

    class Path(val path: String, val category: String) {
        var lastSend = 0L
        var skipCounter = 0
        var calls = ArrayList<Call>()
        val errors = ArrayList<Error>()
    }

    var sender: ((MetricPacket) -> Unit)? = null
    private val callThreads = ThreadLocal<HashMap<String, Path>>()

    private val statNames = arrayOfNulls<String>(maxStats)
    private val statTypes = ByteArray(maxStats)
    private val statScopes = ByteArray(maxStats)
    private val statUnits = Array(maxStats) { MetricUnit.CountUnit }
    private val statsValues = AtomicLongArray(maxStats)
    private var statCount = 0
    @Volatile private var statsSent = 0L
    private var statsUpdating = AtomicBoolean(false)

    /**
     * Registers a new statistic with the provided accumulator type.
     * @return An id that can be used for later calls concerning this statistic.
     */
    @Synchronized fun registerStat(category: String, type: Accumulator, scope: Scope, unit: MetricUnit = MetricUnit.CountUnit): Int {
        val currentIndex = statNames.indexOf(category)
        if(currentIndex < 0) {
            val index = statCount++
            if(index >= statNames.size) throw IllegalStateException("Too many statistic types.")

            statNames[index] = category
            statTypes[index] = type.ordinal.toByte()
            statScopes[index] = scope.ordinal.toByte()
            statUnits[index] = unit
            return index
        } else if(statTypes[currentIndex] !== type.ordinal.toByte()) {
            throw IllegalStateException("Statistic $category already exists under a different type.")
        } else {
            return currentIndex
        }
    }

    /** Updates the numeric statistic for the provided id. */
    fun setStat(id: Int, value: Long) {
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

        val time = System.currentTimeMillis()
        if(time - statsSent > 60*1000 && !statsUpdating.get()) {
            if(statsUpdating.compareAndSet(false, true)) {
                statsSent = time
                sendStats()
                statsUpdating.set(false)
            }
        }
    }

    /** Logs a custom error packet. */
    fun logError(category: String, path: String, reason: String, description: String, trace: String, fatal: Boolean) {
        sender?.invoke(ErrorPacket(path, category, fatal, DateTime.now(), reason, description, trace, 1))
    }

    /** Indicates the start of a new action. All timed actions performed from this thread are added to the action. */
    fun start(path: String, category: String, nextData: Any?): Any? {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        val thread = callThreads.getOrSet { HashMap() }
        val pathData = thread.getOrAdd(path) { Path(path, category) }
        val calls = pathData.calls
        val count = pathData.calls.size

        if(count < 20) {
            val call = Call(pathData, category, startDate, startTime, nextData)
            calls.add(call)
            return call
        } else {
            val stride = if(count < 50) 2 else if(count < 100) 4 else if(count < 1000) 10 else 100
            if(pathData.skipCounter == 0) {
                val call = Call(pathData, category, startDate, startTime, nextData)
                pathData.calls.add(call)
                return call
            }
            pathData.skipCounter++
            if(pathData.skipCounter >= stride) {
                pathData.skipCounter = 0
            }
        }

        // If we have enough metrics already, we just return the path.
        // This allows us to log errors without allocating a lot of data.
        return if(nextData === null) pathData else pathData to nextData
    }

    /** Indicates that the current action has completed. It is added to the metrics. */
    fun finish(call: Any?) {
        if(call !is Call) return

        val path = call.path
        val time = System.nanoTime()
        call.endTime = time

        if((time - path.lastSend > 60000000000L) || (call.failed && time - path.lastSend > 10000000000L)) {
            sendMetrics(path)

            val remaining = ArrayList<Call>()
            path.calls.filterTo(remaining) { it.endTime == 0L }
            path.calls = remaining
            path.lastSend = time
        }
    }

    /** Discards the current action without adding to the metrics, for example if the call was forwarded. */
    fun discard(call: Any?) {
        if(call is Call) {
            call.endTime = System.nanoTime()
            call.failed = true
        }
    }

    /**
     * Indicates that the provided call did not complete correctly.
     * Incorrect calls are not used in the metrics.
     * @param wasError If set, the failure is reported in the metrics as an internal server error.
     */
    fun failCall(call: Call, wasError: Boolean, reason: String, trace: String) {
        if(wasError) {
            addError(call.path, call.startDate, reason, "", true, trace)
        }

        call.failed = true
        finish(call)
    }

    /**
     * Indicates that the provided path failed in an unknown call.
     * @param wasError If set, the failure is reported in the metrics as an internal server error.
     */
    fun failPath(path: Path, wasError: Boolean, reason: String, trace: String) {
        if(wasError) {
            addError(path, DateTime.now(), reason, "", true, trace)
        }
    }

    /**
     * Indicates that an error occurred within a call,
     * but that the error did not cause the path to fail.
     * Most of the time this means that the resource state is correct but a side-effect of the call failed.
     */
    fun error(call: Any, category: String, reason: String, description: String = "") {
        if(call is Call) {
            addError(call.path, call.startDate, reason, description, false, "")
        } else if(call is Path) {
            addError(call, DateTime.now(), reason, description, false, "")
        } else if(call is Pair<*, *> && call.first is Path) {
            addError(call.first, DateTime.now(), reason, description, false, "")
        } else {
            logError(category, "", reason, description, "", false)
        }
    }

    override fun startEvent(call: Any?, type: String, description: String): Int {
        if(call !is Call) return -1

        val startTime = System.nanoTime()
        call.events.add(Event(type, description, startTime, startTime))
        return call.events.size - 1
    }

    override fun endEvent(call: Any?, id: Int) {
        if(call !is Call || id < 0) return

        val event = call.events[id]
        call.events[id] = event.copy(endTime = System.nanoTime())
    }

    override fun onEvent(call: Any?, type: String, description: String, elapsedNanos: Long) {
        if(call !is Call) return

        val endTime = System.nanoTime()
        call.events.add(Event(type, description, endTime - elapsedNanos, endTime))
    }

    private fun sendMetrics(path: Path) {
        // The sender may throw exceptions, which we don't want to leak.
        try {
            sender?.let { sender ->
                // Filter out any failed requests. If it was an internal failure we log it.
                val calls = ArrayList<Call>(path.calls.size)
                path.calls.filterTo(calls) {
                    !it.failed && it.endTime > 0
                }

                path.errors.forEach { error ->
                    sender(ErrorPacket(
                        path.path, path.category, error.fatal, error.start,
                        error.reason, error.description, error.trace, error.count
                    ))
                }
                path.errors.clear()

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
                sender(StatPacket(
                    min.path.path, min.category, date, calls.size, MetricUnit.TimeUnit, totalTime,
                    min.endTime - min.startTime,
                    max.endTime - max.startTime,
                    median.endTime - median.startTime,
                    average95.endTime - average95.startTime,
                    average99.endTime - average99.startTime
                ))

                // Send a full profile for the median and maximum values.
                sender(ProfilePacket(median.path.path, median.category, median.startDate, median.startTime, median.endTime, median.events))
                sender(ProfilePacket(max.path.path, max.category, max.startDate, max.startTime, max.endTime, max.events))
            }
        } catch(e: Throwable) {
            println("Could not send metrics:")
            e.printStackTrace()
        }
    }

    private fun sendStats() {
        // This iteration will be based on information that can be updated while iterating.
        // That should not be a problem, however, since categories are only ever added
        // and the values themselves will at most be sent in a different batch while still being accounted for.
        sender?.let { sender ->
            val time = DateTime.now()
            for(i in 0..statCount - 1) {
                val category = statNames[i] ?: ""
                val scope = statScopes[i]

                // Reset the stat value if it has local scope.
                val value = if(scope == Scope.local.ordinal.toByte()) {
                    statsValues.getAndSet(i, 0)
                } else {
                    statsValues[i]
                }
                sender(StatPacket(null, category, time, 1, statUnits[i], value, value, value, value, value, value))
            }
        }
    }

    private fun addError(
        path: Path, time: DateTime, reason: String,
        description: String, fatal: Boolean, trace: String
    ) {
        val error = path.errors.find { it.fatal == fatal && it.reason == reason && it.description == description }
        if(error == null) {
            path.errors.add(Error(time, path, description, reason, trace, fatal))
        } else {
            error.count++
        }
    }
}