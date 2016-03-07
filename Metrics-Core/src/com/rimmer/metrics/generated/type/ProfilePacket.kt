package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfilePacket(
    val path: String,
    val server: String,
    val time: DateTime,
    val start: Long,
    val end: Long,
    val events: ArrayList<Event>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(pathFieldName)
        encoder.value(path)
        encoder.field(serverFieldName)
        encoder.value(server)
        encoder.field(timeFieldName)
        encoder.value(time)
        encoder.field(startFieldName)
        encoder.value(start)
        encoder.field(endFieldName)
        encoder.value(end)
        encoder.field(eventsFieldName)
        encoder.startArray()
        for(o in events) {
            o.encodeJson(buffer)
        }
        encoder.endArray()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeString(path)
        buffer.writeFieldId(2)
        buffer.writeString(server)
        buffer.writeFieldId(3)
        buffer.writeVarLong(time.millis)
        buffer.writeFieldId(4)
        buffer.writeVarLong(start)
        buffer.writeFieldId(5)
        buffer.writeVarLong(end)
        buffer.writeFieldId(6)
        buffer.writeVarInt(events.size)
        for(o in events) {
            o.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ProfilePacket::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val pathFieldName = "path".toByteArray()
        val pathFieldHash = "path".hashCode()
        val serverFieldName = "server".toByteArray()
        val serverFieldHash = "server".hashCode()
        val timeFieldName = "time".toByteArray()
        val timeFieldHash = "time".hashCode()
        val startFieldName = "start".toByteArray()
        val startFieldHash = "start".hashCode()
        val endFieldName = "end".toByteArray()
        val endFieldHash = "end".hashCode()
        val eventsFieldName = "events".toByteArray()
        val eventsFieldHash = "events".hashCode()

        fun fromBinary(buffer: ByteBuf): ProfilePacket {
            var path: String? = null
            var server: String? = null
            var time: DateTime? = null
            var start: Long = 0L
            var end: Long = 0L
            var events: ArrayList<Event> = ArrayList()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        path = buffer.readString()
                    }
                    2 -> {
                        server = buffer.readString()
                    }
                    3 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                    }
                    4 -> {
                        start = buffer.readVarLong()
                    }
                    5 -> {
                        end = buffer.readVarLong()
                    }
                    6 -> {
                        val length_events = buffer.readVarInt()
                        val i_events = 0
                        while(i_events < length_events) {
                            events.add(Event.fromBinary(buffer))
                        }
                    }
                }
            }
            if(path == null || server == null || time == null) {
                throw InvalidStateException("ProfilePacket instance is missing required fields")
            }
            return ProfilePacket(path, server, time, start, end, events)
        }

        fun fromJson(buffer: ByteBuf): ProfilePacket {
            val token = JsonToken(buffer)
            var path: String? = null
            var server: String? = null
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
                        pathFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            path = token.stringPayload
                        }
                        serverFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            server = token.stringPayload
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
                                events.add(Event.fromJson(buffer))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(path == null || server == null || time == null) {
                throw InvalidStateException("ProfilePacket instance is missing required fields")
            }
            return ProfilePacket(path, server, time, start, end, events)
        }
    }
}
