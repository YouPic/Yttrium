package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class Error(
    val path: String,
    val latest: DateTime,
    val count: Int,
    val cause: String,
    val trace: String
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(pathFieldName)
        writer.value(this.path)
        writer.field(latestFieldName)
        writer.value(this.latest)
        writer.field(countFieldName)
        writer.value(this.count)
        writer.field(causeFieldName)
        writer.value(this.cause)
        writer.field(traceFieldName)
        writer.value(this.trace)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 148064
        buffer.writeVarInt(header0)
        buffer.writeString(path)
        buffer.writeVarLong(latest.millis)
        buffer.writeVarInt(count)
        buffer.writeString(cause)
        buffer.writeString(trace)
    }

    companion object {
        val reader = Reader(Error::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): Error {
            var path: String? = null
            var latest: DateTime? = null
            var count: Int = 0
            var cause: String? = null
            var trace: String? = null

            buffer.readObject {
                when(it) {
                    0 -> {
                        path = buffer.readString()
                        true
                    }
                    1 -> {
                        latest = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    2 -> {
                        count = buffer.readVarInt()
                        true
                    }
                    3 -> {
                        cause = buffer.readString()
                        true
                    }
                    4 -> {
                        trace = buffer.readString()
                        true
                    }
                    else -> false
                }
            }

            if(path == null || latest == null || cause == null || trace == null) {
                throw InvalidStateException("Error instance is missing required fields")
            }
            return Error(path!!, latest!!, count, cause!!, trace!!)
        }

        fun fromJson(token: JsonToken): Error {
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
                        else -> token.skipValue()
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
