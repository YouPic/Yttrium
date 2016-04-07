package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class StatEntry(
    val median: Float,
    val average: Float,
    val average95: Float,
    val average99: Float,
    val max: Float,
    val min: Float,
    val count: Int
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(medianFieldName)
        encoder.value(median)
        encoder.field(averageFieldName)
        encoder.value(average)
        encoder.field(average95FieldName)
        encoder.value(average95)
        encoder.field(average99FieldName)
        encoder.value(average99)
        encoder.field(maxFieldName)
        encoder.value(max)
        encoder.field(minFieldName)
        encoder.value(min)
        encoder.field(countFieldName)
        encoder.value(count)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeFloat(median)
        buffer.writeFieldId(2)
        buffer.writeFloat(average)
        buffer.writeFieldId(3)
        buffer.writeFloat(average95)
        buffer.writeFieldId(4)
        buffer.writeFloat(average99)
        buffer.writeFieldId(5)
        buffer.writeFloat(max)
        buffer.writeFieldId(6)
        buffer.writeFloat(min)
        buffer.writeFieldId(7)
        buffer.writeVarInt(count)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(StatEntry::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val medianFieldName = "median".toByteArray()
        val medianFieldHash = "median".hashCode()
        val averageFieldName = "average".toByteArray()
        val averageFieldHash = "average".hashCode()
        val average95FieldName = "average95".toByteArray()
        val average95FieldHash = "average95".hashCode()
        val average99FieldName = "average99".toByteArray()
        val average99FieldHash = "average99".hashCode()
        val maxFieldName = "max".toByteArray()
        val maxFieldHash = "max".hashCode()
        val minFieldName = "min".toByteArray()
        val minFieldHash = "min".hashCode()
        val countFieldName = "count".toByteArray()
        val countFieldHash = "count".hashCode()

        fun fromBinary(buffer: ByteBuf): StatEntry {
            var median: Float = 0f
            var average: Float = 0f
            var average95: Float = 0f
            var average99: Float = 0f
            var max: Float = 0f
            var min: Float = 0f
            var count: Int = 0
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        median = buffer.readFloat()
                    }
                    2 -> {
                        average = buffer.readFloat()
                    }
                    3 -> {
                        average95 = buffer.readFloat()
                    }
                    4 -> {
                        average99 = buffer.readFloat()
                    }
                    5 -> {
                        max = buffer.readFloat()
                    }
                    6 -> {
                        min = buffer.readFloat()
                    }
                    7 -> {
                        count = buffer.readVarInt()
                    }
                }
            }
            return StatEntry(median, average, average95, average99, max, min, count)
        }

        fun fromJson(buffer: ByteBuf): StatEntry {
            val token = JsonToken(buffer)
            var median: Float = 0f
            var average: Float = 0f
            var average95: Float = 0f
            var average99: Float = 0f
            var max: Float = 0f
            var min: Float = 0f
            var count: Int = 0
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        medianFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            median = token.numberPayload.toFloat()
                        }
                        averageFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average = token.numberPayload.toFloat()
                        }
                        average95FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average95 = token.numberPayload.toFloat()
                        }
                        average99FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average99 = token.numberPayload.toFloat()
                        }
                        maxFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            max = token.numberPayload.toFloat()
                        }
                        minFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            min = token.numberPayload.toFloat()
                        }
                        countFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            count = token.numberPayload.toInt()
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return StatEntry(median, average, average95, average99, max, min, count)
        }
    }
}
