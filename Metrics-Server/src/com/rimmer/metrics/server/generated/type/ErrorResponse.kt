package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class ErrorResponse(
    val errors: List<Error>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(errorsFieldName)
        writer.startArray()
        for(o in this.errors) {
            writer.arrayField()
            o.encodeJson(writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 56
        buffer.writeVarInt(header0)
        buffer.writeVarLong((errors.size.toLong() shl 3) or 5)
        for(o in errors) {
            o.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(ErrorResponse::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ErrorResponse {
            var errors: ArrayList<Error> = ArrayList()

            buffer.readObject {
                when(it) {
                    0 -> {
                        val length_errors = buffer.readVarLong() ushr 3
                        var i_errors = 0
                        while(i_errors < length_errors) {
                            errors.add(Error.fromBinary(buffer))
                            i_errors++
                        }
                        true
                    }
                    else -> false
                }
            }

            return ErrorResponse(errors)
        }

        fun fromJson(token: JsonToken): ErrorResponse {
            var errors: ArrayList<Error> = ArrayList()
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        errorsFieldHash -> {
                            token.expect(JsonToken.Type.StartArray)
                            while(!token.peekArrayEnd()) {
                                errors.add(Error.fromJson(token))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ErrorResponse(errors)
        }
    }
}
