package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

interface MetricPacket: Writable {

    companion object {
        val reader = Reader(MetricPacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun readBinaryHeader(buffer: ByteBuf): Int {
            val start = buffer.readerIndex()
            buffer.readVarInt()
            val type = buffer.readVarInt()
            buffer.readerIndex(start)
            return type
        }

        fun fromBinary(buffer: ByteBuf): MetricPacket {
            return when(readBinaryHeader(buffer)) {
                0 -> StatPacket.fromBinary(buffer)
                1 -> ProfilePacket.fromBinary(buffer)
                2 -> ErrorPacket.fromBinary(buffer)
                else -> throw InvalidStateException("Unknown MetricPacket ordinal")
            }
        }

        fun readJsonHeader(token: JsonToken): String {
            val start = token.buffer.readerIndex()
            token.expect(JsonToken.Type.StartObject)

            while(true) {
                token.expect(JsonToken.Type.FieldName)
                if(token.stringPayload == "dataType") {
                    token.expect(JsonToken.Type.StringLit)
                    token.buffer.readerIndex(start)
                    return token.stringPayload
                } else {
                    token.skipValue()
                }
            }
        }

        fun fromJson(token: JsonToken): MetricPacket {
            return when(readJsonHeader(token)) {
                "StatPacket" -> StatPacket.fromJson(token)
                "ProfilePacket" -> ProfilePacket.fromJson(token)
                "ErrorPacket" -> ErrorPacket.fromJson(token)
                else -> throw InvalidStateException("Unknown MetricPacket ordinal")
            }
        }
    }
}

data class StatPacket(
    val location: String?,
    val category: String,
    val time: DateTime,
    val sampleCount: Int,
    val total: Long,
    val min: Long,
    val max: Long,
    val median: Long,
    val average95: Long,
    val average99: Long
): Writable, MetricPacket {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(dataTypeStringFieldName)
        writer.value(dataTypeString)
        if(location !== null) {
            writer.field(locationFieldName)
            writer.value(location)
        }
        writer.field(categoryFieldName)
        writer.value(category)
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(sampleCountFieldName)
        writer.value(sampleCount)
        writer.field(totalFieldName)
        writer.value(total)
        writer.field(minFieldName)
        writer.value(min)
        writer.field(maxFieldName)
        writer.value(max)
        writer.field(medianFieldName)
        writer.value(median)
        writer.field(average95FieldName)
        writer.value(average95)
        writer.field(average99FieldName)
        writer.value(average99)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 1227133729
        if(location == null) {
            header0 = header0 and -57
        }
        buffer.writeVarInt(header0)
        buffer.writeVarInt(dataType)
        if(location !== null) {
            buffer.writeString(location)
        }
        buffer.writeString(category)
        buffer.writeVarLong(time.millis)
        buffer.writeVarInt(sampleCount)
        buffer.writeVarLong(total)
        buffer.writeVarLong(min)
        buffer.writeVarLong(max)
        buffer.writeVarLong(median)
        buffer.writeVarLong(average95)
        var header10 = 1
        buffer.writeVarInt(header10)
        buffer.writeVarLong(average99)
    }

    companion object {
        val dataType = 0
        val dataTypeString = "StatPacket"
        val dataTypeStringFieldName = "dataType"

        fun fromBinary(buffer: ByteBuf): StatPacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var sampleCount: Int = 0
            var total: Long = 0L
            var min: Long = 0L
            var max: Long = 0L
            var median: Long = 0L
            var average95: Long = 0L
            var average99: Long = 0L

            buffer.readObject {
                when(it) {
                    1 -> {
                        location = buffer.readString()
                        true
                    }
                    2 -> {
                        category = buffer.readString()
                        true
                    }
                    3 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    4 -> {
                        sampleCount = buffer.readVarInt()
                        true
                    }
                    5 -> {
                        total = buffer.readVarLong()
                        true
                    }
                    6 -> {
                        min = buffer.readVarLong()
                        true
                    }
                    7 -> {
                        max = buffer.readVarLong()
                        true
                    }
                    8 -> {
                        median = buffer.readVarLong()
                        true
                    }
                    9 -> {
                        average95 = buffer.readVarLong()
                        true
                    }
                    10 -> {
                        average99 = buffer.readVarLong()
                        true
                    }
                    else -> false
                }
            }

            if(category == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(location, category!!, time!!, sampleCount, total, min, max, median, average95, average99)
        }

        fun fromJson(token: JsonToken): StatPacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var sampleCount: Int = 0
            var total: Long = 0L
            var min: Long = 0L
            var max: Long = 0L
            var median: Long = 0L
            var average95: Long = 0L
            var average99: Long = 0L
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
                        sampleCountFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            sampleCount = token.numberPayload.toInt()
                        }
                        totalFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            total = token.numberPayload.toLong()
                        }
                        minFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            min = token.numberPayload.toLong()
                        }
                        maxFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            max = token.numberPayload.toLong()
                        }
                        medianFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            median = token.numberPayload.toLong()
                        }
                        average95FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average95 = token.numberPayload.toLong()
                        }
                        average99FieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            average99 = token.numberPayload.toLong()
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(category == null || time == null) {
                throw InvalidStateException("StatPacket instance is missing required fields")
            }
            return StatPacket(location, category, time, sampleCount, total, min, max, median, average95, average99)
        }
    }
}

data class ProfilePacket(
    val location: String?,
    val category: String,
    val time: DateTime,
    val start: Long,
    val end: Long,
    val events: List<Event>
): Writable, MetricPacket {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(dataTypeStringFieldName)
        writer.value(dataTypeString)
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
        var header0 = 1872673
        if(location == null) {
            header0 = header0 and -57
        }
        buffer.writeVarInt(header0)
        buffer.writeVarInt(dataType)
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
        val dataType = 1
        val dataTypeString = "ProfilePacket"
        val dataTypeStringFieldName = "dataType"

        fun fromBinary(buffer: ByteBuf): ProfilePacket {
            var location: String? = null
            var category: String? = null
            var time: DateTime? = null
            var start: Long = 0L
            var end: Long = 0L
            var events: ArrayList<Event> = ArrayList()

            buffer.readObject {
                when(it) {
                    1 -> {
                        location = buffer.readString()
                        true
                    }
                    2 -> {
                        category = buffer.readString()
                        true
                    }
                    3 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    4 -> {
                        start = buffer.readVarLong()
                        true
                    }
                    5 -> {
                        end = buffer.readVarLong()
                        true
                    }
                    6 -> {
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

data class ErrorPacket(
    val location: String?,
    val category: String,
    val fatal: Boolean,
    val time: DateTime,
    val cause: String,
    val description: String,
    val trace: String,
    val count: Int
): Writable, MetricPacket {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(dataTypeStringFieldName)
        writer.value(dataTypeString)
        if(location !== null) {
            writer.field(locationFieldName)
            writer.value(location)
        }
        writer.field(categoryFieldName)
        writer.value(category)
        writer.field(fatalFieldName)
        writer.value(fatal)
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(causeFieldName)
        writer.value(cause)
        writer.field(descriptionFieldName)
        writer.value(description)
        writer.field(traceFieldName)
        writer.value(trace)
        writer.field(countFieldName)
        writer.value(count)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 26350369
        if(location == null) {
            header0 = header0 and -57
        }
        buffer.writeVarInt(header0)
        buffer.writeVarInt(dataType)
        if(location !== null) {
            buffer.writeString(location)
        }
        buffer.writeString(category)
        buffer.writeVarInt(if(fatal) 1 else 0)
        buffer.writeVarLong(time.millis)
        buffer.writeString(cause)
        buffer.writeString(description)
        buffer.writeString(trace)
        buffer.writeVarInt(count)
    }

    companion object {
        val dataType = 2
        val dataTypeString = "ErrorPacket"
        val dataTypeStringFieldName = "dataType"

        fun fromBinary(buffer: ByteBuf): ErrorPacket {
            var location: String? = null
            var category: String? = null
            var fatal: Boolean = false
            var time: DateTime? = null
            var cause: String? = null
            var description: String? = null
            var trace: String? = null
            var count: Int = 0

            buffer.readObject {
                when(it) {
                    1 -> {
                        location = buffer.readString()
                        true
                    }
                    2 -> {
                        category = buffer.readString()
                        true
                    }
                    3 -> {
                        fatal = buffer.readVarInt().let {it != 0}
                        true
                    }
                    4 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    5 -> {
                        cause = buffer.readString()
                        true
                    }
                    6 -> {
                        description = buffer.readString()
                        true
                    }
                    7 -> {
                        trace = buffer.readString()
                        true
                    }
                    8 -> {
                        count = buffer.readVarInt()
                        true
                    }
                    else -> false
                }
            }

            if(category == null || time == null || cause == null || description == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(location, category!!, fatal, time!!, cause!!, description!!, trace!!, count)
        }

        fun fromJson(token: JsonToken): ErrorPacket {
            var location: String? = null
            var category: String? = null
            var fatal: Boolean = false
            var time: DateTime? = null
            var cause: String? = null
            var description: String? = null
            var trace: String? = null
            var count: Int = 0
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
                        fatalFieldHash -> {
                            token.expect(JsonToken.Type.BoolLit)
                            fatal = token.boolPayload
                        }
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
                        causeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            cause = token.stringPayload
                        }
                        descriptionFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            description = token.stringPayload
                        }
                        traceFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            trace = token.stringPayload
                        }
                        countFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            count = token.numberPayload.toInt()
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(category == null || time == null || cause == null || description == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(location, category, fatal, time, cause, description, trace, count)
        }
    }
}

