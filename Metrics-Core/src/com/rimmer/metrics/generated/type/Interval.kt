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
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(startFieldName)
        encoder.value(start)
        encoder.field(endFieldName)
        encoder.value(end)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeVarLong(start)
        buffer.writeFieldId(2)
        buffer.writeVarLong(end)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(Interval::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val startFieldName = "start".toByteArray()
        val startFieldHash = "start".hashCode()
        val endFieldName = "end".toByteArray()
        val endFieldHash = "end".hashCode()

        fun fromBinary(buffer: ByteBuf): Interval {
            var start: Long = 0L
            var end: Long = 0L
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        start = buffer.readVarLong()
                    }
                    2 -> {
                        end = buffer.readVarLong()
                    }
                }
            }
            return Interval(start, end)
        }

        fun fromJson(buffer: ByteBuf): Interval {
            val token = JsonToken(buffer)
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
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return Interval(start, end)
        }
    }
}
