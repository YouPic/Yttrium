package com.rimmer.yttrium.mysql

import com.rimmer.metrics.Metrics
import com.rimmer.mysql.protocol.QueryListener
import com.rimmer.mysql.protocol.QueryResult

class MetricsQueryListener(val metrics: Metrics): QueryListener {
    override fun onQuery(id: Long, query: String, result: QueryResult?, error: Throwable?) {
        metrics.onEvent(id.toInt(), "MySQL", query, result?.elapsed ?: 0L)
    }
}