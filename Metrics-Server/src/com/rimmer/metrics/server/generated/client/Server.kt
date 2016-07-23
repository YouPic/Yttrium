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

fun BinaryClient.serverMetric(packets: List<MetricPacket>, callback: (Unit?, Throwable?) -> Unit) {
    call(
        821579075, {
            val header0 = 56
            this.writeVarInt(header0)
            this.writeVarLong((packets.size.toLong() shl 3) or 5)
            for(o in packets) {
                o.encodeBinary(this)
            }
        }, {
        }, callback
    )
}

fun BinaryClient.serverMetric(packets: List<MetricPacket>): Task<Unit> {
    val resultTask = Task<Unit>()
    serverMetric(packets, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

