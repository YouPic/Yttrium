package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class ProfileEvent(
    val group: String,
    val event: String,
    val startTime: Long,
    val endTime: Long
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(groupFieldName)
        writer.value(this.group)
        writer.field(eventFieldName)
        writer.value(this.event)
        writer.field(startTimeFieldName)
        writer.value(this.startTime)
        writer.field(endTimeFieldName)
        writer.value(this.endTime)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 4896
        buffer.writeVarInt(header0)
        buffer.writeString(group)
        buffer.writeString(event)
        buffer.writeVarLong(startTime)
        buffer.writeVarLong(endTime)
    }

    companion object {
        val reader = Reader(ProfileEvent::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ProfileEvent {
            var group: String? = null
            var event: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L

            buffer.readObject {
                when(it) {
                    0 -> {
                        group = buffer.readString()
                        true
                    }
                    1 -> {
                        event = buffer.readString()
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

            if(group == null || event == null) {
                throw InvalidStateException("ProfileEvent instance is missing required fields")
            }
            return ProfileEvent(group!!, event!!, startTime, endTime)
        }

        fun fromJson(token: JsonToken): ProfileEvent {
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
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(group == null || event == null) {
                throw InvalidStateException("ProfileEvent instance is missing required fields")
            }
            return ProfileEvent(group, event, startTime, endTime)
        }
    }
}
