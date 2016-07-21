package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ErrorPacket(
    val location: String?,
    val category: String,
    val fatal: Boolean,
    val time: DateTime,
    val cause: String,
    val description: String,
    val trace: String
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
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
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 9573152
        if(location == null) {
            header0 = header0 and -8
        }
        buffer.writeVarInt(header0)
        if(location !== null) {
            buffer.writeString(location)
        }
        buffer.writeString(category)
        buffer.writeVarInt(if(fatal) 1 else 0)
        buffer.writeVarLong(time.millis)
        buffer.writeString(cause)
        buffer.writeString(description)
        buffer.writeString(trace)
    }

    companion object {
        val reader = Reader(ErrorPacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ErrorPacket {
            var location: String? = null
            var category: String? = null
            var fatal: Boolean = false
            var time: DateTime? = null
            var cause: String? = null
            var description: String? = null
            var trace: String? = null

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
                        fatal = buffer.readVarInt().let {it != 0}
                        true
                    }
                    3 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    4 -> {
                        cause = buffer.readString()
                        true
                    }
                    5 -> {
                        description = buffer.readString()
                        true
                    }
                    6 -> {
                        trace = buffer.readString()
                        true
                    }
                    else -> false
                }
            }

            if(category == null || time == null || cause == null || description == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(location, category!!, fatal, time!!, cause!!, description!!, trace!!)
        }

        fun fromJson(token: JsonToken): ErrorPacket {
            var location: String? = null
            var category: String? = null
            var fatal: Boolean = false
            var time: DateTime? = null
            var cause: String? = null
            var description: String? = null
            var trace: String? = null
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
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(category == null || time == null || cause == null || description == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(location, category, fatal, time, cause, description, trace)
        }
    }
}
