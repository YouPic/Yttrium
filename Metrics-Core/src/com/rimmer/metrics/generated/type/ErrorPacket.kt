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
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(pathFieldName)
        encoder.value(path)
        encoder.field(timeFieldName)
        encoder.value(time)
        encoder.field(causeFieldName)
        encoder.value(cause)
        encoder.field(traceFieldName)
        encoder.value(trace)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeString(path)
        buffer.writeFieldId(2)
        buffer.writeVarLong(time.millis)
        buffer.writeFieldId(3)
        buffer.writeString(cause)
        buffer.writeFieldId(4)
        buffer.writeString(trace)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ErrorPacket::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val pathFieldName = "path".toByteArray()
        val pathFieldHash = "path".hashCode()
        val timeFieldName = "time".toByteArray()
        val timeFieldHash = "time".hashCode()
        val causeFieldName = "cause".toByteArray()
        val causeFieldHash = "cause".hashCode()
        val traceFieldName = "trace".toByteArray()
        val traceFieldHash = "trace".hashCode()

        fun fromBinary(buffer: ByteBuf): ErrorPacket {
            var path: String? = null
            var time: DateTime? = null
            var cause: String? = null
            var trace: String? = null
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        path = buffer.readString()
                    }
                    2 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                    }
                    3 -> {
                        cause = buffer.readString()
                    }
                    4 -> {
                        trace = buffer.readString()
                    }
                }
            }
            if(path == null || time == null || cause == null || trace == null) {
                throw InvalidStateException("ErrorPacket instance is missing required fields")
            }
            return ErrorPacket(path, time, cause, trace)
        }

        fun fromJson(buffer: ByteBuf): ErrorPacket {
            val token = JsonToken(buffer)
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
