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
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(pathFieldName)
        writer.value(this.path)
        writer.field(serverFieldName)
        writer.value(this.server)
        writer.field(timeFieldName)
        writer.value(this.time)
        writer.field(totalElapsedFieldName)
        writer.value(this.totalElapsed)
        writer.field(intervalsFieldName)
        writer.startArray()
        for(o in this.intervals) {
            writer.arrayField()
            o.encodeJson(writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 234272
        buffer.writeVarInt(header0)
        buffer.writeString(path)
        buffer.writeString(server)
        buffer.writeVarLong(time.millis)
        buffer.writeVarLong(totalElapsed)
        buffer.writeVarLong((intervals.size.toLong() shl 3) or 5)
        for(o in intervals) {
            o.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(StatPacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): StatPacket {
            var path: String? = null
            var server: String? = null
            var time: DateTime? = null
            var totalElapsed: Long = 0L
            var intervals: ArrayList<Interval> = ArrayList()

            buffer.readObject {
                when(it) {
                    0 -> {
                        path = buffer.readString()
                        true
                    }
                    1 -> {
                        server = buffer.readString()
                        true
                    }
                    2 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    3 -> {
                        totalElapsed = buffer.readVarLong()
                        true
                    }
                    4 -> {
                        val length_intervals = buffer.readVarLong() ushr 3
                        var i_intervals = 0
                        while(i_intervals < length_intervals) {
                            intervals.add(Interval.fromBinary(buffer))
                            i_intervals++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(path == null || server == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(path!!, server!!, time!!, totalElapsed, intervals)
        }

        fun fromJson(token: JsonToken): StatPacket {
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
                                intervals.add(Interval.fromJson(token))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                        else -> token.skipValue()
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
