package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class Event(
    val type: String,
    val description: String,
    val startTime: Long,
    val endTime: Long
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(typeFieldName)
        writer.value(type)
        writer.field(descriptionFieldName)
        writer.value(description)
        writer.field(startTimeFieldName)
        writer.value(startTime)
        writer.field(endTimeFieldName)
        writer.value(endTime)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 4896
        buffer.writeVarInt(header0)
        buffer.writeString(type)
        buffer.writeString(description)
        buffer.writeVarLong(startTime)
        buffer.writeVarLong(endTime)
    }

    companion object {
        val reader = Reader(Event::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): Event {
            var type: String? = null
            var description: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L

            buffer.readObject {
                when(it) {
                    0 -> {
                        type = buffer.readString()
                        true
                    }
                    1 -> {
                        description = buffer.readString()
                        true
                    }
                    2 -> {
                        startTime = buffer.readVarLong()
                        true
                    }
                    3 -> {
                        endTime = buffer.readVarLong()
                        true
                    }
                    else -> false
                }
            }

            if(type == null || description == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(type!!, description!!, startTime, endTime)
        }

        fun fromJson(token: JsonToken): Event {
            var type: String? = null
            var description: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        typeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            type = token.stringPayload
                        }
                        descriptionFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            description = token.stringPayload
                        }
                        startTimeFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            startTime = token.numberPayload.toLong()
                        }
                        endTimeFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            endTime = token.numberPayload.toLong()
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(type == null || description == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(type, description, startTime, endTime)
        }
    }
}
