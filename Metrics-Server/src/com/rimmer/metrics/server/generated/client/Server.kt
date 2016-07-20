package com.rimmer.metrics.server.generated.client

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.metrics.server.generated.type.*

fun BinaryClient.statistic(stat: StatPacket, callback: (Unit?, Throwable?) -> Unit) {
    call(
        1431278813, {
            val header0 = 40
            this.writeVarInt(header0)
            stat.encodeBinary(this)
        }, {
        }, callback
    )
}

fun BinaryClient.statistic(stat: StatPacket): Task<Unit> {
    val resultTask = Task<Unit>()
    statistic(stat, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.error(error: ErrorPacket, callback: (Unit?, Throwable?) -> Unit) {
    call(
        435141653, {
            val header0 = 40
            this.writeVarInt(header0)
            error.encodeBinary(this)
        }, {
        }, callback
    )
}

fun BinaryClient.error(error: ErrorPacket): Task<Unit> {
    val resultTask = Task<Unit>()
    error(error, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.profile(profile: ProfilePacket, callback: (Unit?, Throwable?) -> Unit) {
    call(
        -1566104458, {
            val header0 = 40
            this.writeVarInt(header0)
            profile.encodeBinary(this)
        }, {
        }, callback
    )
}

fun BinaryClient.profile(profile: ProfilePacket): Task<Unit> {
    val resultTask = Task<Unit>()
    profile(profile, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

