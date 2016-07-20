package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ErrorPacket(
    val errors: List<Error>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(errorsFieldName)
        encoder.startArray()
        for(o in errors) {
            encoder.arrayField()
            o.encodeJson(buffer)
        }
        encoder.endArray()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeVarInt(errors.size)
        for(o in errors) {
            o.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ErrorPacket::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val errorsFieldName = "errors".toByteArray()
        val errorsFieldHash = "errors".hashCode()

        fun fromBinary(buffer: ByteBuf): ErrorPacket {
            var errors: ArrayList<Error> = ArrayList()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        val length_errors = buffer.readVarInt()
                        var i_errors = 0
                        while(i_errors < length_errors) {
                            errors.add(Error.fromBinary(buffer))
                            i_errors++
                        }
                    }
                }
            }
            return ErrorPacket(errors)
        }

        fun fromJson(buffer: ByteBuf): ErrorPacket {
            val token = JsonToken(buffer)
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
                                errors.add(Error.fromJson(buffer))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ErrorPacket(errors)
        }
    }
}
