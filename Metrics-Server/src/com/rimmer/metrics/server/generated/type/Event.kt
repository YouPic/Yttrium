package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class Event(
    val group: String,
    val event: String,
    val startTime: Long,
    val endTime: Long
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(groupFieldName)
        encoder.value(group)
        encoder.field(eventFieldName)
        encoder.value(event)
        encoder.field(startTimeFieldName)
        encoder.value(startTime)
        encoder.field(endTimeFieldName)
        encoder.value(endTime)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeString(group)
        buffer.writeFieldId(2)
        buffer.writeString(event)
        buffer.writeFieldId(3)
        buffer.writeVarLong(startTime)
        buffer.writeFieldId(4)
        buffer.writeVarLong(endTime)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(Event::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val groupFieldName = "group".toByteArray()
        val groupFieldHash = "group".hashCode()
        val eventFieldName = "event".toByteArray()
        val eventFieldHash = "event".hashCode()
        val startTimeFieldName = "startTime".toByteArray()
        val startTimeFieldHash = "startTime".hashCode()
        val endTimeFieldName = "endTime".toByteArray()
        val endTimeFieldHash = "endTime".hashCode()

        fun fromBinary(buffer: ByteBuf): Event {
            var group: String? = null
            var event: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        group = buffer.readString()
                    }
                    2 -> {
                        event = buffer.readString()
                    }
                    3 -> {
                        startTime = buffer.readVarLong()
                    }
                    4 -> {
                        endTime = buffer.readVarLong()
                    }
                }
            }
            if(group == null || event == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(group, event, startTime, endTime)
        }

        fun fromJson(buffer: ByteBuf): Event {
            val token = JsonToken(buffer)
            var group: String? = null
            var event: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        groupFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            group = token.stringPayload
                        }
                        eventFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            event = token.stringPayload
                        }
                        startTimeFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            startTime = token.numberPayload.toLong()
                        }
                        endTimeFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            endTime = token.numberPayload.toLong()
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(group == null || event == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(group, event, startTime, endTime)
        }
    }
}
