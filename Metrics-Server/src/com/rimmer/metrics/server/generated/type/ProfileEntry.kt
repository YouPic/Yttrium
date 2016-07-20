package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfileEntry(
    val start: Long,
    val end: Long,
    val events: List<Event>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(startFieldName)
        encoder.value(start)
        encoder.field(endFieldName)
        encoder.value(end)
        encoder.field(eventsFieldName)
        encoder.startArray()
        for(o in events) {
            encoder.arrayField()
            o.encodeJson(buffer)
        }
        encoder.endArray()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeVarLong(start)
        buffer.writeFieldId(2)
        buffer.writeVarLong(end)
        buffer.writeFieldId(3)
        buffer.writeVarInt(events.size)
        for(o in events) {
            o.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ProfileEntry::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val startFieldName = "start".toByteArray()
        val startFieldHash = "start".hashCode()
        val endFieldName = "end".toByteArray()
        val endFieldHash = "end".hashCode()
        val eventsFieldName = "events".toByteArray()
        val eventsFieldHash = "events".hashCode()

        fun fromBinary(buffer: ByteBuf): ProfileEntry {
            var start: Long = 0L
            var end: Long = 0L
            var events: ArrayList<Event> = ArrayList()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        start = buffer.readVarLong()
                    }
                    2 -> {
                        end = buffer.readVarLong()
                    }
                    3 -> {
                        val length_events = buffer.readVarInt()
                        var i_events = 0
                        while(i_events < length_events) {
                            events.add(Event.fromBinary(buffer))
                            i_events++
                        }
                    }
                }
            }
            return ProfileEntry(start, end, events)
        }

        fun fromJson(buffer: ByteBuf): ProfileEntry {
            val token = JsonToken(buffer)
            var start: Long = 0L
            var end: Long = 0L
            var events: ArrayList<Event> = ArrayList()
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
                                events.add(Event.fromJson(buffer))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ProfileEntry(start, end, events)
        }
    }
}
