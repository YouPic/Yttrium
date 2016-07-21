package com.rimmer.metrics.server.generated.client

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.metrics.server.generated.type.*

fun BinaryClient.clientGetStats(from: Long, to: Long, password: String, callback: (StatResponse?, Throwable?) -> Unit) {
    call(
        -1537045299, {
            val header0 = 2120
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeVarLong(to)
            this.writeString(password)
        }, {
            StatResponse.fromBinary(this)
        }, callback
    )
}

fun BinaryClient.clientGetStats(from: Long, to: Long, password: String): Task<StatResponse> {
    val resultTask = Task<StatResponse>()
    clientGetStats(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.clientGetProfile(from: Long, to: Long, password: String, callback: (ProfileResponse?, Throwable?) -> Unit) {
    call(
        198807319, {
            val header0 = 2120
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeVarLong(to)
            this.writeString(password)
        }, {
            ProfileResponse.fromBinary(this)
        }, callback
    )
}

fun BinaryClient.clientGetProfile(from: Long, to: Long, password: String): Task<ProfileResponse> {
    val resultTask = Task<ProfileResponse>()
    clientGetProfile(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.clientGetError(from: Long, password: String, callback: (ErrorResponse?, Throwable?) -> Unit) {
    call(
        -1958242560, {
            val header0 = 264
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeString(password)
        }, {
            ErrorResponse.fromBinary(this)
        }, callback
    )
}

fun BinaryClient.clientGetError(from: Long, password: String): Task<ErrorResponse> {
    val resultTask = Task<ErrorResponse>()
    clientGetError(from, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

