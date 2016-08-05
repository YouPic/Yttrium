package com.rimmer.metrics.server.generated.client

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.metrics.server.generated.type.*

fun BinaryClient.serverMetric(packets: List<MetricPacket>, serverName: String, callback: (Unit?, Throwable?) -> Unit) {
    call(
        -1500814561, {
            val header0 = 39
            this.writeVarInt(header0)
            this.writeVarLong((packets.size.toLong() shl 3) or 5)
            for(o in packets) {
                o.encodeBinary(this)
            }
            this.writeString(serverName)
        }, {
        }, callback
    )
}

fun BinaryClient.serverMetric(packets: List<MetricPacket>, serverName: String): Task<Unit> {
    val resultTask = Task<Unit>()
    serverMetric(packets, serverName, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

