package com.rimmer.yttrium.mysql

import com.rimmer.mysql.pool.PoolConfiguration
import com.rimmer.mysql.pool.SingleThreadPool
import com.rimmer.mysql.protocol.Connection
import com.rimmer.mysql.protocol.QueryResult
import com.rimmer.yttrium.Context
import com.rimmer.yttrium.Task
import com.rimmer.yttrium.server.ServerContext
import io.netty.channel.EventLoop
import io.netty.channel.EventLoopGroup
import java.util.*

/** A connection pool that manages one single-thread connection pool for each event loop in a group. */
class SQLPool(
    context: ServerContext,
    val config: PoolConfiguration,
    val creator: (EventLoopGroup, (Connection?, Throwable?) -> Unit) -> Unit
) {
    private val pool: Map<EventLoop, SingleThreadPool>

    init {
        // We initialize the pool at creation to avoid synchronization when lazily creating pools.
        val map = HashMap<EventLoop, SingleThreadPool>()
        context.handlerGroup.forEach { loop ->
            if(loop is EventLoop) {
                map[loop] = SingleThreadPool(config) { creator(loop, it) }
            }
        }

        pool = map
    }

    operator fun get(context: Context): SingleThreadPool {
        return pool[context.eventLoop] ?:
            throw IllegalArgumentException("Invalid context event loop - the event loop must be in the server's handlerGroup.")
    }
}

fun SQLPool.query(context: Context, query: String, args: List<Any?>, types: List<Class<*>>? = null): Task<QueryResult> {
    val task = Task<QueryResult>()
    this[context].get { c, e ->
        if(e == null) {
            c!!.query(query, args, types, context.listenerData, false) { r, e ->
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

fun SQLPool.query(context: Context, query: String, vararg params: Any?, types: List<Class<*>>? = null) =
    query(context, query, Arrays.asList(*params), types)

fun SQLPool.textQuery(context: Context, query: String, types: List<Class<*>>? = null): Task<QueryResult> {
    val task = Task<QueryResult>()
    this[context].get { c, e ->
        if(e == null) {
            c!!.query(query, emptyList(), types, context.listenerData, true) { r, e ->
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