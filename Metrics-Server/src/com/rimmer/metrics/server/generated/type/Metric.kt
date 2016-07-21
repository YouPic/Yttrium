package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class Metric(
    val median: Float,
    val average: Float,
    val average95: Float,
    val average99: Float,
    val max: Float,
    val min: Float,
    val count: Int
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(medianFieldName)
        writer.value(median)
        writer.field(averageFieldName)
        writer.value(average)
        writer.field(average95FieldName)
        writer.value(average95)
        writer.field(average99FieldName)
        writer.value(average99)
        writer.field(maxFieldName)
        writer.value(max)
        writer.field(minFieldName)
        writer.value(min)
        writer.field(countFieldName)
        writer.value(count)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 2696336
        buffer.writeVarInt(header0)
        buffer.writeFloat(median)
        buffer.writeFloat(average)
        buffer.writeFloat(average95)
        buffer.writeFloat(average99)
        buffer.writeFloat(max)
        buffer.writeFloat(min)
        buffer.writeVarInt(count)
    }

    companion object {
        val reader = Reader(Metric::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): Metric {
            var median: Float = 0f
            var average: Float = 0f
            var average95: Float = 0f
            var average99: Float = 0f
            var max: Float = 0f
            var min: Float = 0f
            var count: Int = 0

            buffer.readObject {
                when(it) {
                    0 -> {
                        median = buffer.readFloat()
                        true
                    }
                    1 -> {
                        average = buffer.readFloat()
                        true
                    }
                    2 -> {
                        average95 = buffer.readFloat()
                        true
                    }
                    3 -> {
                        average99 = buffer.readFloat()
                        true
                    }
                    4 -> {
                        max = buffer.readFloat()
                        true
                    }
                    5 -> {
                        min = buffer.readFloat()
                        true
                    }
                    6 -> {
                        count = buffer.readVarInt()
                        true
                    }
                    else -> false
                }
            }

            return Metric(median, average, average95, average99, max, min, count)
        }

        fun fromJson(token: JsonToken): Metric {
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
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return Metric(median, average, average95, average99, max, min, count)
        }
    }
}
