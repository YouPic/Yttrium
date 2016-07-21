package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class ErrorInstance(
    val time: DateTime,
    val trace: String,
    val path: String?
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(traceFieldName)
        writer.value(trace)
        if(path !== null) {
            writer.field(pathFieldName)
            writer.value(path)
        }
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        var header0 = 2312
        if(path == null) {
            header0 = header0 and -449
        }
        buffer.writeVarInt(header0)
        buffer.writeVarLong(time.millis)
        buffer.writeString(trace)
        if(path !== null) {
            buffer.writeString(path)
        }
    }

    companion object {
        val reader = Reader(ErrorInstance::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ErrorInstance {
            var time: DateTime? = null
            var trace: String? = null
            var path: String? = null

            buffer.readObject {
                when(it) {
                    0 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    1 -> {
                        trace = buffer.readString()
                        true
                    }
                    2 -> {
                        path = buffer.readString()
                        true
                    }
                    else -> false
                }
            }

            if(time == null || trace == null) {
                throw InvalidStateException("ErrorInstance instance is missing required fields")
            }
            return ErrorInstance(time!!, trace!!, path)
        }

        fun fromJson(token: JsonToken): ErrorInstance {
            var time: DateTime? = null
            var trace: String? = null
            var path: String? = null
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        timeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            time = DateTime.parse(token.stringPayload)
                        }
                        traceFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            trace = token.stringPayload
                        }
                        pathFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            path = token.stringPayload
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(time == null || trace == null) {
                throw InvalidStateException("ErrorInstance instance is missing required fields")
            }
            return ErrorInstance(time, trace, path)
        }
    }
}
