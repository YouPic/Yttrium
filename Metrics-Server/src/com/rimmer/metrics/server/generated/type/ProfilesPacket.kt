package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfilesPacket(
    val slices: List<ProfileSlice>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(slicesFieldName)
        encoder.startArray()
        for(o in slices) {
            encoder.arrayField()
            o.encodeJson(buffer)
        }
        encoder.endArray()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeVarInt(slices.size)
        for(o in slices) {
            o.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ProfilesPacket::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val slicesFieldName = "slices".toByteArray()
        val slicesFieldHash = "slices".hashCode()

        fun fromBinary(buffer: ByteBuf): ProfilesPacket {
            var slices: ArrayList<ProfileSlice> = ArrayList()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        val length_slices = buffer.readVarInt()
                        var i_slices = 0
                        while(i_slices < length_slices) {
                            slices.add(ProfileSlice.fromBinary(buffer))
                            i_slices++
                        }
                    }
                }
            }
            return ProfilesPacket(slices)
        }

        fun fromJson(buffer: ByteBuf): ProfilesPacket {
            val token = JsonToken(buffer)
            var slices: ArrayList<ProfileSlice> = ArrayList()
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
                                slices.add(ProfileSlice.fromJson(buffer))
                            }
                            token.expect(JsonToken.Type.EndArray)
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            return ProfilesPacket(slices)
        }
    }
}
