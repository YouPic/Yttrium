package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class ProfileResponse(
    val slices: List<ProfileSlice>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(slicesFieldName)
        writer.startArray()
        for(o in this.slices) {
            writer.arrayField()
            o.encodeJson(writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 56
        buffer.writeVarInt(header0)
        buffer.writeVarLong((slices.size.toLong() shl 3) or 5)
        for(o in slices) {
            o.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(ProfileResponse::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ProfileResponse {
            val slices: ArrayList<ProfileSlice> = ArrayList()

            buffer.readObject {
                when(it) {
                    0 -> {
                        val length_slices = buffer.readVarLong() ushr 3
                        var i_slices = 0
                        while(i_slices < length_slices) {
                            slices.add(ProfileSlice.fromBinary(buffer))
                            i_slices++
                        }
                        true
                    }
                    else -> false
                }
            }

            return ProfileResponse(slices)
        }

        fun fromJson(token: JsonToken): ProfileResponse {
            val slices: ArrayList<ProfileSlice> = ArrayList()
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        slicesFieldHash -> {
                            token.expect(JsonToken.Type.StartArray)
                            while(!token.peekArrayEnd()) {
                                slices.add(ProfileSlice.fromJson(token))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ProfileResponse(slices)
        }
    }
}
