package com.rimmer.yttrium.metrics

import com.rimmer.metrics.Metrics
import com.rimmer.metrics.generated.type.EventType
import com.rimmer.mysql.protocol.QueryListener
import com.rimmer.mysql.protocol.QueryResult

class MetricsQueryListener(val metrics: Metrics): QueryListener {
    override fun onQuery(id: Long, query: String, result: QueryResult?, error: Throwable?) {
        metrics.onEvent(id.toInt(), EventType.MySQL, query, result?.elapsed ?: 0L)
    }
}