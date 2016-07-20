package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class Error(
    val path: String,
    val latest: DateTime,
    val count: Int,
    val cause: String,
    val trace: String
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(pathFieldName)
        encoder.value(path)
        encoder.field(latestFieldName)
        encoder.value(latest)
        encoder.field(countFieldName)
        encoder.value(count)
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
        buffer.writeVarLong(latest.millis)
        buffer.writeFieldId(3)
        buffer.writeVarInt(count)
        buffer.writeFieldId(4)
        buffer.writeString(cause)
        buffer.writeFieldId(5)
        buffer.writeString(trace)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(Error::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val pathFieldName = "path".toByteArray()
        val pathFieldHash = "path".hashCode()
        val latestFieldName = "latest".toByteArray()
        val latestFieldHash = "latest".hashCode()
        val countFieldName = "count".toByteArray()
        val countFieldHash = "count".hashCode()
        val causeFieldName = "cause".toByteArray()
        val causeFieldHash = "cause".hashCode()
        val traceFieldName = "trace".toByteArray()
        val traceFieldHash = "trace".hashCode()

        fun fromBinary(buffer: ByteBuf): Error {
            var path: String? = null
            var latest: DateTime? = null
            var count: Int = 0
            var cause: String? = null
            var trace: String? = null
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        path = buffer.readString()
                    }
                    2 -> {
                        latest = buffer.readVarLong().let {DateTime(it)}
                    }
                    3 -> {
                        count = buffer.readVarInt()
                    }
                    4 -> {
                        cause = buffer.readString()
                    }
                    5 -> {
                        trace = buffer.readString()
                    }
                }
            }
            if(path == null || latest == null || cause == null || trace == null) {
                throw InvalidStateException("Error instance is missing required fields")
            }
            return Error(path, latest, count, cause, trace)
        }

        fun fromJson(buffer: ByteBuf): Error {
            val token = JsonToken(buffer)
            var path: String? = null
            var latest: DateTime? = null
            var count: Int = 0
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
                        latestFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            latest = DateTime.parse(token.stringPayload)
                        }
                        countFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            count = token.numberPayload.toInt()
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
            if(path == null || latest == null || cause == null || trace == null) {
                throw InvalidStateException("Error instance is missing required fields")
            }
            return Error(path, latest, count, cause, trace)
        }
    }
}
