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

fun BinaryClient.serverStatistic(stats: List<StatPacket>, callback: (Unit?, Throwable?) -> Unit) {
    call(
        1431278813, {
            val header0 = 56
            this.writeVarInt(header0)
            this.writeVarLong((stats.size.toLong() shl 3) or 5)
            for(o in stats) {
                o.encodeBinary(this)
            }
        }, {
        }, callback
    )
}

fun BinaryClient.serverStatistic(stats: List<StatPacket>): Task<Unit> {
    val resultTask = Task<Unit>()
    serverStatistic(stats, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.serverError(errors: List<ErrorPacket>, callback: (Unit?, Throwable?) -> Unit) {
    call(
        435141653, {
            val header0 = 56
            this.writeVarInt(header0)
            this.writeVarLong((errors.size.toLong() shl 3) or 5)
            for(o in errors) {
                o.encodeBinary(this)
            }
        }, {
        }, callback
    )
}

fun BinaryClient.serverError(errors: List<ErrorPacket>): Task<Unit> {
    val resultTask = Task<Unit>()
    serverError(errors, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.serverProfile(profiles: List<ProfilePacket>, callback: (Unit?, Throwable?) -> Unit) {
    call(
        -1566104458, {
            val header0 = 56
            this.writeVarInt(header0)
            this.writeVarLong((profiles.size.toLong() shl 3) or 5)
            for(o in profiles) {
                o.encodeBinary(this)
            }
        }, {
        }, callback
    )
}

fun BinaryClient.serverProfile(profiles: List<ProfilePacket>): Task<Unit> {
    val resultTask = Task<Unit>()
    serverProfile(profiles, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

