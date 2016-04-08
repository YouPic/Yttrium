package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class StatPacket(
    val path: String,
    val server: String,
    val time: DateTime,
    val totalElapsed: Long,
    val intervals: List<Interval>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(pathFieldName)
        encoder.value(path)
        encoder.field(serverFieldName)
        encoder.value(server)
        encoder.field(timeFieldName)
        encoder.value(time)
        encoder.field(totalElapsedFieldName)
        encoder.value(totalElapsed)
        encoder.field(intervalsFieldName)
        encoder.startArray()
        for(o in intervals) {
            encoder.arrayField()
            o.encodeJson(buffer)
        }
        encoder.endArray()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeString(path)
        buffer.writeFieldId(2)
        buffer.writeString(server)
        buffer.writeFieldId(3)
        buffer.writeVarLong(time.millis)
        buffer.writeFieldId(4)
        buffer.writeVarLong(totalElapsed)
        buffer.writeFieldId(5)
        buffer.writeVarInt(intervals.size)
        for(o in intervals) {
            o.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(StatPacket::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val pathFieldName = "path".toByteArray()
        val pathFieldHash = "path".hashCode()
        val serverFieldName = "server".toByteArray()
        val serverFieldHash = "server".hashCode()
        val timeFieldName = "time".toByteArray()
        val timeFieldHash = "time".hashCode()
        val totalElapsedFieldName = "totalElapsed".toByteArray()
        val totalElapsedFieldHash = "totalElapsed".hashCode()
        val intervalsFieldName = "intervals".toByteArray()
        val intervalsFieldHash = "intervals".hashCode()

        fun fromBinary(buffer: ByteBuf): StatPacket {
            var path: String? = null
            var server: String? = null
            var time: DateTime? = null
            var totalElapsed: Long = 0L
            var intervals: ArrayList<Interval> = ArrayList()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        path = buffer.readString()
                    }
                    2 -> {
                        server = buffer.readString()
                    }
                    3 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                    }
                    4 -> {
                        totalElapsed = buffer.readVarLong()
                    }
                    5 -> {
                        val length_intervals = buffer.readVarInt()
                        val i_intervals = 0
                        while(i_intervals < length_intervals) {
                            intervals.add(Interval.fromBinary(buffer))
                            i_intervals++
                        }
                    }
                }
            }
            if(path == null || server == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(path, server, time, totalElapsed, intervals)
        }

        fun fromJson(buffer: ByteBuf): StatPacket {
            val token = JsonToken(buffer)
            var path: String? = null
            var server: String? = null
            var time: DateTime? = null
            var totalElapsed: Long = 0L
            var intervals: ArrayList<Interval> = ArrayList()
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        pathFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            path = token.stringPayload
                        }
                        serverFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            server = token.stringPayload
                        }
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
                        totalElapsedFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            totalElapsed = token.numberPayload.toLong()
                        }
                        intervalsFieldHash -> {
                            token.expect(JsonToken.Type.StartArray)
                            while(!token.peekArrayEnd()) {
                                intervals.add(Interval.fromJson(buffer))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(path == null || server == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(path, server, time, totalElapsed, intervals)
        }
    }
}
