package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class ProfileEntry(
    val start: Long,
    val end: Long,
    val events: List<ProfileEvent>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(startFieldName)
        writer.value(this.start)
        writer.field(endFieldName)
        writer.value(this.end)
        writer.field(eventsFieldName)
        writer.startArray()
        for(o in this.events) {
            writer.arrayField()
            o.encodeJson(writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 3656
        buffer.writeVarInt(header0)
        buffer.writeVarLong(start)
        buffer.writeVarLong(end)
        buffer.writeVarLong((events.size.toLong() shl 3) or 5)
        for(o in events) {
            o.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(ProfileEntry::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ProfileEntry {
            var start: Long = 0L
            var end: Long = 0L
            val events: ArrayList<ProfileEvent> = ArrayList()

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
                    2 -> {
                        val length_events = buffer.readVarLong() ushr 3
                        var i_events = 0
                        while(i_events < length_events) {
                            events.add(ProfileEvent.fromBinary(buffer))
                            i_events++
                        }
                        true
                    }
                    else -> false
                }
            }

            return ProfileEntry(start, end, events)
        }

        fun fromJson(token: JsonToken): ProfileEntry {
            var start: Long = 0L
            var end: Long = 0L
            val events: ArrayList<ProfileEvent> = ArrayList()
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
                        eventsFieldHash -> {
                            token.expect(JsonToken.Type.StartArray)
                            while(!token.peekArrayEnd()) {
                                events.add(ProfileEvent.fromJson(token))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ProfileEntry(start, end, events)
        }
    }
}
