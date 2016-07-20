package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class ProfileStat(
    val normal: ProfileEntry,
    val max: ProfileEntry
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(normalFieldName)
        normal.encodeJson(writer)
        writer.field(maxFieldName)
        max.encodeJson(writer)
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 360
        buffer.writeVarInt(header0)
        normal.encodeBinary(buffer)
        max.encodeBinary(buffer)
    }

    companion object {
        val reader = Reader(ProfileStat::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ProfileStat {
            var normal: ProfileEntry? = null
            var max: ProfileEntry? = null

            buffer.readObject {
                when(it) {
                    0 -> {
                        normal = ProfileEntry.fromBinary(buffer)
                        true
                    }
                    1 -> {
                        max = ProfileEntry.fromBinary(buffer)
                        true
                    }
                    else -> false
                }
            }

            if(normal == null || max == null) {
                throw InvalidStateException("ProfileStat instance is missing required fields")
            }
            return ProfileStat(normal!!, max!!)
        }

        fun fromJson(token: JsonToken): ProfileStat {
            var normal: ProfileEntry? = null
            var max: ProfileEntry? = null
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        normalFieldHash -> {
                            normal = ProfileEntry.fromJson(token)
                        }
                        maxFieldHash -> {
                            max = ProfileEntry.fromJson(token)
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(normal == null || max == null) {
                throw InvalidStateException("ProfileStat instance is missing required fields")
            }
            return ProfileStat(normal, max)
        }
    }
}
