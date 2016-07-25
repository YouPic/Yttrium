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

fun BinaryClient.clientGetStats(from: Long, to: Long, password: String, callback: (List<TimeMetric>?, Throwable?) -> Unit) {
    call(
        896073641, {
            val header0 = 265
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeVarLong(to)
            this.writeString(password)
        }, {
            val value: ArrayList<TimeMetric> = ArrayList()
            val length_value = this.readVarLong() ushr 3
            var i_value = 0
            while(i_value < length_value) {
                value!!.add(TimeMetric.fromBinary(this))
                i_value++
            }
            value
        }, callback
    )
}

fun BinaryClient.clientGetStats(from: Long, to: Long, password: String): Task<List<TimeMetric>> {
    val resultTask = Task<List<TimeMetric>>()
    clientGetStats(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.clientGetProfile(from: Long, to: Long, password: String, callback: (List<TimeProfile>?, Throwable?) -> Unit) {
    call(
        1963899635, {
            val header0 = 265
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeVarLong(to)
            this.writeString(password)
        }, {
            val value: ArrayList<TimeProfile> = ArrayList()
            val length_value = this.readVarLong() ushr 3
            var i_value = 0
            while(i_value < length_value) {
                value!!.add(TimeProfile.fromBinary(this))
                i_value++
            }
            value
        }, callback
    )
}

fun BinaryClient.clientGetProfile(from: Long, to: Long, password: String): Task<List<TimeProfile>> {
    val resultTask = Task<List<TimeProfile>>()
    clientGetProfile(from, to, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

fun BinaryClient.clientGetError(from: Long, password: String, callback: (List<ErrorClass>?, Throwable?) -> Unit) {
    call(
        -423376220, {
            val header0 = 33
            this.writeVarInt(header0)
            this.writeVarLong(from)
            this.writeString(password)
        }, {
            val value: ArrayList<ErrorClass> = ArrayList()
            val length_value = this.readVarLong() ushr 3
            var i_value = 0
            while(i_value < length_value) {
                value!!.add(ErrorClass.fromBinary(this))
                i_value++
            }
            value
        }, callback
    )
}

fun BinaryClient.clientGetError(from: Long, password: String): Task<List<ErrorClass>> {
    val resultTask = Task<List<ErrorClass>>()
    clientGetError(from, password, { r, e -> if(e === null) resultTask.finish(r!!) else resultTask.fail(e) })
    return resultTask
}

