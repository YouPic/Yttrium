package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

data class ProfileSlice(
    val time: DateTime,
    val paths: Map<String, ProfileStat>
): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        val encoder = JsonWriter(buffer)
        encoder.startObject()
        encoder.field(timeFieldName)
        encoder.value(time)
        encoder.field(pathsFieldName)
        encoder.startObject()
        for(kv in paths) {
            encoder.field(kv.key)
            kv.value.encodeJson(buffer)
        }
        encoder.endObject()
        encoder.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeFieldId(1)
        buffer.writeVarLong(time.millis)
        buffer.writeFieldId(2)
        buffer.writeVarInt(paths.size)
        for(kv in paths) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
        buffer.endObject()
    }

    companion object {
        init {
            registerReadable(ProfileSlice::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        val timeFieldName = "time".toByteArray()
        val timeFieldHash = "time".hashCode()
        val pathsFieldName = "paths".toByteArray()
        val pathsFieldHash = "paths".hashCode()

        fun fromBinary(buffer: ByteBuf): ProfileSlice {
            var time: DateTime? = null
            var paths: HashMap<String, ProfileStat> = HashMap()
            
            loop@ while(true) {
                when(buffer.readFieldId()) {
                    0 -> break@loop
                    1 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                    }
                    2 -> {
                        val length_paths = buffer.readVarInt()
                        var i_paths = 0
                        while(i_paths < length_paths) {
                            val
                            paths_k = buffer.readString()
                            val
                            paths_v = ProfileStat.fromBinary(buffer)
                            paths.put(paths_k, paths_v)
                            i_paths++
                        }
                    }
                }
            }
            if(time == null) {
                throw InvalidStateException("ProfileSlice instance is missing required fields")
            }
            return ProfileSlice(time, paths)
        }

        fun fromJson(buffer: ByteBuf): ProfileSlice {
            val token = JsonToken(buffer)
            var time: DateTime? = null
            var paths: HashMap<String, ProfileStat> = HashMap()
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
                        pathsFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val paths_k = token.stringPayload
                                    val
                                    paths_v = ProfileStat.fromJson(buffer)
                                    paths.put(paths_k, paths_v)
                                } else {
                                    throw InvalidStateException("Invalid json: expected field or object end")
                                }
                            }
                        }
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(time == null) {
                throw InvalidStateException("ProfileSlice instance is missing required fields")
            }
            return ProfileSlice(time, paths)
        }
    }
}
