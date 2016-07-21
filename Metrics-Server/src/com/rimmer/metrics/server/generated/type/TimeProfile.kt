package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class TimeProfile(
    val time: DateTime,
    val servers: Map<String, HashMap<String, CategoryProfile>>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(serversFieldName)
        writer.startObject()
        for(kv in servers) {
            writer.field(kv.key)
            writer.startObject()
            for(kv in kv.value) {
                writer.field(kv.key)
                kv.value.encodeJson(writer)
            }
            writer.endObject()
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 392
        buffer.writeVarInt(header0)
        buffer.writeVarLong(time.millis)
        buffer.writeVarLong((servers.size.toLong() shl 6) or 4 or (6 shl 3))
        for(kv in servers) {
            buffer.writeString(kv.key)
            buffer.writeVarLong((kv.value.size.toLong() shl 6) or 4 or (5 shl 3))
            for(kv in kv.value) {
                buffer.writeString(kv.key)
                kv.value.encodeBinary(buffer)
            }
        }
    }

    companion object {
        val reader = Reader(TimeProfile::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): TimeProfile {
            var time: DateTime? = null
            var servers: HashMap<String, HashMap<String, CategoryProfile>> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    1 -> {
                        val length_servers = buffer.readVarLong() ushr 6
                        var i_servers = 0
                        while(i_servers < length_servers) {
                            val
                            servers_k = buffer.readString()
                            val servers_v = HashMap<String, CategoryProfile>()
                            val length_servers_v = buffer.readVarLong() ushr 6
                            var i_servers_v = 0
                            while(i_servers_v < length_servers_v) {
                                val
                                servers_v_k = buffer.readString()
                                val
                                servers_v_v = CategoryProfile.fromBinary(buffer)
                                servers_v.put(servers_v_k, servers_v_v)
                                i_servers_v++
                            }
                            servers.put(servers_k, servers_v)
                            i_servers++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(time == null) {
                throw InvalidStateException("TimeProfile instance is missing required fields")
            }
            return TimeProfile(time!!, servers)
        }

        fun fromJson(token: JsonToken): TimeProfile {
            var time: DateTime? = null
            var servers: HashMap<String, HashMap<String, CategoryProfile>> = HashMap()
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
                        serversFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val servers_k = token.stringPayload
                                    val servers_v = HashMap<String, CategoryProfile>()
                                    token.expect(JsonToken.Type.StartObject)
                                    while(true) {
                                        token.parse()
                                        if(token.type == JsonToken.Type.EndObject) {
                                            break
                                        } else if(token.type == JsonToken.Type.FieldName) {
                                            val servers_v_k = token.stringPayload
                                            val
                                            servers_v_v = CategoryProfile.fromJson(token)
                                            servers_v.put(servers_v_k, servers_v_v)
                                        } else {
                                            throw InvalidStateException("Invalid json: expected field or object end")
                                        }
                                    }
                                    servers.put(servers_k, servers_v)
                                } else {
                                    throw InvalidStateException("Invalid json: expected field or object end")
                                }
                            }
                        }
                        else -> token.skipValue()
                    }
                } else {
                    throw InvalidStateException("Invalid json: expected field or object end")
                }
            }
            if(time == null) {
                throw InvalidStateException("TimeProfile instance is missing required fields")
            }
            return TimeProfile(time, servers)
        }
    }
}
