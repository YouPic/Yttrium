
package com.rimmer

import org.joda.time.DateTime
import java.util.*

enum class DBType {redis, mysql, mongo}

interface DBMetrics {
    /**
     * Starts a new database query within the current call.
     * @return A query index to send to endQuery.
     */
    fun startQuery(dbType: DBType, type: String): Int

    /** Indicates that the provided query id has finished. */
    fun endQuery(id: Int)
}

class Metrics: DBMetrics {
    class DatabaseCall(val db: DBType, val type: String, val startDate: DateTime, val startTime: Long) {
        var endTime = startTime
    }

    class Call(val path: String, val startDate: DateTime, val startTime: Long) {
        val db = ArrayList<DatabaseCall>()
        var endTime = startTime
        var failed = false
        var error = false
        var failReason = ""
    }

    class Path {
        var lastSend = System.nanoTime()
        val calls = ArrayList<Call>()
    }

    val paths = HashMap<String, Path>()
    val currentCall = ThreadLocal<Call>()

    /** Indicates the start of a new action. All timed actions performed from this thread are added to the action. */
    fun start(path: String) {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        currentCall.set(Call(path, startDate, startTime))
    }

    /** Indicates that the current action has completed. It is added to the metrics. */
    fun finish() {
        val time = System.nanoTime()
        val call = currentCall.get()
        call.endTime = time

        var path = paths[call.path]
        if(path === null) {
            path = Path()
            paths.put(call.path, path)
        }

        path.calls.add(call)
        if(time - path.lastSend > 60000000000L) {
            buildStats(path, time)
            path.calls.clear()
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

    override fun startQuery(dbType: DBType, type: String): Int {
        val startTime = System.nanoTime()
        val startDate = DateTime.now()
        return currentCall.get()?.run {
            db.add(DatabaseCall(dbType, type, startDate, startTime))
            db.size - 1
        } ?: 0
    }

    override fun endQuery(id: Int) {
        currentCall.get()?.run {
            db[id].endTime = System.nanoTime()
        }
    }

    private fun buildStats(path: Path, time: Long) {

    }
}