package com.rimmer.yttrium.mysql

import com.rimmer.mysql.pool.PoolConfiguration
import com.rimmer.mysql.pool.SingleThreadPool
import com.rimmer.mysql.protocol.Connection
import com.rimmer.mysql.protocol.QueryResult
import com.rimmer.yttrium.Context
import com.rimmer.yttrium.Task
import io.netty.channel.EventLoop
import io.netty.channel.EventLoopGroup
import java.util.*

/** A connection pool that manages one single-thread connection pool for each event loop in a group. */
class SQLPool(val config: PoolConfiguration, val creator: (EventLoopGroup, (Connection?, Throwable?) -> Unit) -> Unit) {
    private val pool = HashMap<EventLoop, SingleThreadPool>()

    operator fun get(context: Context): SingleThreadPool {
        val loop = context.eventLoop
        val c = cached(loop)
        if(c == null) {
            val p = SingleThreadPool(config) { creator(loop, it) }
            pool[loop] = p
            return p
        } else return c
    }

    fun cached(eventLoop: EventLoop): SingleThreadPool? {
        return pool[eventLoop]
    }
}

fun SQLPool.query(context: Context, query: String, vararg params: Any?, types: List<Class<*>>? = null): Task<QueryResult> {
    val task = Task<QueryResult>()
    this[context].get { c, e ->
        if(e == null) {
            c!!.query(query, Arrays.asList(*params), types, context.id) { r, e ->
                c.disconnect()
                if (e == null) {
                    task.finish(r!!)
                } else {
                    task.fail(e)
                }
            }
        } else {
            task.fail(e)
        }
    }
    return task
}