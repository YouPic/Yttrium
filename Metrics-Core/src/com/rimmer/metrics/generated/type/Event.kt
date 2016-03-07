package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class Event(
    val event: EventType,
    val type: String,
    val startDate: DateTime,
    val startTime: Long,
    val endTime: Long
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(eventFieldName)
        event.encodeJson(buffer)
        encoder.field(typeFieldName)
        encoder.value(type)
        encoder.field(startDateFieldName)
        encoder.value(startDate)
        encoder.field(startTimeFieldName)
        encoder.value(startTime)
        encoder.field(endTimeFieldName)
        encoder.value(endTime)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        event.encodeBinary(buffer)
        buffer.writeFieldId(2)
        buffer.writeString(type)
        buffer.writeFieldId(3)
        buffer.writeVarLong(startDate.millis)
        buffer.writeFieldId(4)
        buffer.writeVarLong(startTime)
        buffer.writeFieldId(5)
        buffer.writeVarLong(endTime)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(Event::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val eventFieldName = "event".toByteArray()
        val eventFieldHash = "event".hashCode()
        val typeFieldName = "type".toByteArray()
        val typeFieldHash = "type".hashCode()
        val startDateFieldName = "startDate".toByteArray()
        val startDateFieldHash = "startDate".hashCode()
        val startTimeFieldName = "startTime".toByteArray()
        val startTimeFieldHash = "startTime".hashCode()
        val endTimeFieldName = "endTime".toByteArray()
        val endTimeFieldHash = "endTime".hashCode()

        fun fromBinary(buffer: ByteBuf): Event {
            var event: EventType? = null
            var type: String? = null
            var startDate: DateTime? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        event = EventType.fromBinary(buffer)
                    }
                    2 -> {
                        type = buffer.readString()
                    }
                    3 -> {
                        startDate = buffer.readVarLong().let {DateTime(it)}
                    }
                    4 -> {
                        startTime = buffer.readVarLong()
                    }
                    5 -> {
                        endTime = buffer.readVarLong()
                    }
                }
            }
            if(event == null || type == null || startDate == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(event, type, startDate, startTime, endTime)
        }

        fun fromJson(buffer: ByteBuf): Event {
            val token = JsonToken(buffer)
            var event: EventType? = null
            var type: String? = null
            var startDate: DateTime? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        eventFieldHash -> {
                            event = EventType.fromJson(buffer)
                        }
                        typeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            type = token.stringPayload
                        }
                        startDateFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            startDate = DateTime.parse(token.stringPayload)
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
            if(event == null || type == null || startDate == null) {
                throw InvalidStateException("Event instance is missing required fields")
            }
            return Event(event, type, startDate, startTime, endTime)
        }
    }
}
