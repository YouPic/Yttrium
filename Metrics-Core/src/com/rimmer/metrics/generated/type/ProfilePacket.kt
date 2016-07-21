package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfilePacket(
    val location: String?,
    val category: String,
    val time: DateTime,
    val start: Long,
    val end: Long,
    val events: List<Event>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        if(location !== null) {
            writer.field(locationFieldName)
            writer.value(location)
        }
        writer.field(categoryFieldName)
        writer.value(category)
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(startFieldName)
        writer.value(start)
        writer.field(endFieldName)
        writer.value(end)
        writer.field(eventsFieldName)
        writer.startArray()
        for(o in events) {
            writer.arrayField()
            o.encodeJson(writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 1872672
        if(location == null) {
            header0 = header0 and -8
        }
        buffer.writeVarInt(header0)
        if(location !== null) {
            buffer.writeString(location)
        }
        buffer.writeString(category)
        buffer.writeVarLong(time.millis)
        buffer.writeVarLong(start)
        buffer.writeVarLong(end)
        buffer.writeVarLong((events.size.toLong() shl 3) or 5)
        for(o in events) {
            o.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(ProfilePacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ProfilePacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var start: Long = 0L
            var end: Long = 0L
            var events: ArrayList<Event> = ArrayList()

            buffer.readObject {
                when(it) {
                    0 -> {
                        location = buffer.readString()
                        true
                    }
                    1 -> {
                        category = buffer.readString()
                        true
                    }
                    2 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    3 -> {
                        start = buffer.readVarLong()
                        true
                    }
                    4 -> {
                        end = buffer.readVarLong()
                        true
                    }
                    5 -> {
                        val length_events = buffer.readVarLong() ushr 3
                        var i_events = 0
                        while(i_events < length_events) {
                            events!!.add(Event.fromBinary(buffer))
                            i_events++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(category == null || time == null) {
                throw InvalidStateException("ProfilePacket instance is missing required fields")
            }
            return ProfilePacket(location, category!!, time!!, start, end, events)
        }

        fun fromJson(token: JsonToken): ProfilePacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
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
                        locationFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            location = token.stringPayload
                        }
                        categoryFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            category = token.stringPayload
                        }
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
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
                                events.add(Event.fromJson(token))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(category == null || time == null) {
                throw InvalidStateException("ProfilePacket instance is missing required fields")
            }
            return ProfilePacket(location, category, time, start, end, events)
        }
    }
}
