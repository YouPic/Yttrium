package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class StatPacket(
    val location: String?,
    val category: String,
    val time: DateTime,
    val sampleCount: Int,
    val total: Long,
    val min: Long,
    val max: Long,
    val median: Long,
    val average95: Long,
    val average99: Long
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        if(location !== null) {
            writer.field(locationFieldName)
            writer.value(location)
        }
        writer.field(categoryFieldName)
        writer.value(category)
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(sampleCountFieldName)
        writer.value(sampleCount)
        writer.field(totalFieldName)
        writer.value(total)
        writer.field(minFieldName)
        writer.value(min)
        writer.field(maxFieldName)
        writer.value(max)
        writer.field(medianFieldName)
        writer.value(median)
        writer.field(average95FieldName)
        writer.value(average95)
        writer.field(average99FieldName)
        writer.value(average99)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 1227133728
        if(location == null) {
            header0 = header0 and -8
        }
        buffer.writeVarInt(header0)
        if(location !== null) {
            buffer.writeString(location)
        }
        buffer.writeString(category)
        buffer.writeVarLong(time.millis)
        buffer.writeVarInt(sampleCount)
        buffer.writeVarLong(total)
        buffer.writeVarLong(min)
        buffer.writeVarLong(max)
        buffer.writeVarLong(median)
        buffer.writeVarLong(average95)
        buffer.writeVarLong(average99)
    }

    companion object {
        val reader = Reader(StatPacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): StatPacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var sampleCount: Int = 0
            var total: Long = 0L
            var min: Long = 0L
            var max: Long = 0L
            var median: Long = 0L
            var average95: Long = 0L
            var average99: Long = 0L

            buffer.readObject {
                when(it) {
                    0 -> {
                        location = buffer.readString()
                        true
                    }
                    1 -> {
                        category = buffer.readString()
                        true
                    }
                    2 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    3 -> {
                        sampleCount = buffer.readVarInt()
                        true
                    }
                    4 -> {
                        total = buffer.readVarLong()
                        true
                    }
                    5 -> {
                        min = buffer.readVarLong()
                        true
                    }
                    6 -> {
                        max = buffer.readVarLong()
                        true
                    }
                    7 -> {
                        median = buffer.readVarLong()
                        true
                    }
                    8 -> {
                        average95 = buffer.readVarLong()
                        true
                    }
                    9 -> {
                        average99 = buffer.readVarLong()
                        true
                    }
                    else -> false
                }
            }

            if(category == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(location, category!!, time!!, sampleCount, total, min, max, median, average95, average99)
        }

        fun fromJson(token: JsonToken): StatPacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var sampleCount: Int = 0
            var total: Long = 0L
            var min: Long = 0L
            var max: Long = 0L
            var median: Long = 0L
            var average95: Long = 0L
            var average99: Long = 0L
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        locationFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            location = token.stringPayload
                        }
                        categoryFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            category = token.stringPayload
                        }
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
                        sampleCountFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            sampleCount = token.numberPayload.toInt()
                        }
                        totalFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            total = token.numberPayload.toLong()
                        }
                        minFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            min = token.numberPayload.toLong()
                        }
                        maxFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            max = token.numberPayload.toLong()
                        }
                        medianFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            median = token.numberPayload.toLong()
                        }
                        average95FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average95 = token.numberPayload.toLong()
                        }
                        average99FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average99 = token.numberPayload.toLong()
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(category == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(location, category, time, sampleCount, total, min, max, median, average95, average99)
        }
    }
}
