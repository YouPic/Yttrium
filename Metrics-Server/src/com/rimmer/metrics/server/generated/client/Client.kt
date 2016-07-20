package com.rimmer.metrics.server.generated.client

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.metrics.server.generated.type.*

fun BinaryClient.getStats(from: Long, to: Long, password: String, callback: (StatResponse?, Throwable?) -> Unit) {
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

fun BinaryClient.getStats(from: Long, to: Long, password: String): Task<StatResponse> {
    val resultTask = Task<StatResponse>()
    getStats(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.getProfile(from: Long, to: Long, password: String, callback: (ProfileResponse?, Throwable?) -> Unit) {
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

fun BinaryClient.getProfile(from: Long, to: Long, password: String): Task<ProfileResponse> {
    val resultTask = Task<ProfileResponse>()
    getProfile(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.getError(from: Long, password: String, callback: (ErrorResponse?, Throwable?) -> Unit) {
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

fun BinaryClient.getError(from: Long, password: String): Task<ErrorResponse> {
    val resultTask = Task<ErrorResponse>()
    getError(from, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

