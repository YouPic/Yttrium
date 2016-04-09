package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfileStat(
    val normal: ProfileEntry,
    val max: ProfileEntry
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(normalFieldName)
        normal.encodeJson(buffer)
        encoder.field(maxFieldName)
        max.encodeJson(buffer)
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        normal.encodeBinary(buffer)
        buffer.writeFieldId(2)
        max.encodeBinary(buffer)
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ProfileStat::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val normalFieldName = "normal".toByteArray()
        val normalFieldHash = "normal".hashCode()
        val maxFieldName = "max".toByteArray()
        val maxFieldHash = "max".hashCode()

        fun fromBinary(buffer: ByteBuf): ProfileStat {
            var normal: ProfileEntry? = null
            var max: ProfileEntry? = null
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        normal = ProfileEntry.fromBinary(buffer)
                    }
                    2 -> {
                        max = ProfileEntry.fromBinary(buffer)
                    }
                }
            }
            if(normal == null || max == null) {
                throw InvalidStateException("ProfileStat instance is missing required fields")
            }
            return ProfileStat(normal, max)
        }

        fun fromJson(buffer: ByteBuf): ProfileStat {
            val token = JsonToken(buffer)
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
                            normal = ProfileEntry.fromJson(buffer)
                        }
                        maxFieldHash -> {
                            max = ProfileEntry.fromJson(buffer)
                        }
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
