package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ErrorPacket(
    val path: String,
    val time: DateTime,
    val cause: String,
    val trace: String
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(pathFieldName)
        writer.value(path)
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(causeFieldName)
        writer.value(cause)
        writer.field(traceFieldName)
        writer.value(trace)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 18528
        buffer.writeVarInt(header0)
        buffer.writeString(path)
        buffer.writeVarLong(time.millis)
        buffer.writeString(cause)
        buffer.writeString(trace)
    }

    companion object {
        val reader = Reader(ErrorPacket::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ErrorPacket {
            var path: String? = null
            var time: DateTime? = null
            var cause: String? = null
            var trace: String? = null

            buffer.readObject {
                when(it) {
                    0 -> {
                        path = buffer.readString()
                        true
                    }
                    1 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    2 -> {
                        cause = buffer.readString()
                        true
                    }
                    3 -> {
                        trace = buffer.readString()
                        true
                    }
                    else -> false
                }
            }

            if(path == null || time == null || cause == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(path!!, time!!, cause!!, trace!!)
        }

        fun fromJson(token: JsonToken): ErrorPacket {
            var path: String? = null
            var time: DateTime? = null
            var cause: String? = null
            var trace: String? = null
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
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
                        causeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            cause = token.stringPayload
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
            if(path == null || time == null || cause == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(path, time, cause, trace)
        }
    }
}
