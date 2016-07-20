package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class Interval(
    val start: Long,
    val end: Long
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(startFieldName)
        writer.value(this.start)
        writer.field(endFieldName)
        writer.value(this.end)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 72
        buffer.writeVarInt(header0)
        buffer.writeVarLong(start)
        buffer.writeVarLong(end)
    }

    companion object {
        val reader = Reader(Interval::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): Interval {
            var start: Long = 0L
            var end: Long = 0L

            buffer.readObject {
                when(it) {
                    0 -> {
                        start = buffer.readVarLong()
                        true
                    }
                    1 -> {
                        end = buffer.readVarLong()
                        true
                    }
                    else -> false
                }
            }

            return Interval(start, end)
        }

        fun fromJson(token: JsonToken): Interval {
            var start: Long = 0L
            var end: Long = 0L
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        startFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            start = token.numberPayload.toLong()
                        }
                        endFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            end = token.numberPayload.toLong()
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return Interval(start, end)
        }
    }
}
