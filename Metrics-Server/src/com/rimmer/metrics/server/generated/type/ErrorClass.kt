package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class ErrorClass(
    val cause: String,
    val count: Int,
    val fatal: Boolean,
    val lastError: DateTime,
    val servers: Map<String, ArrayList<ErrorInstance>>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(causeFieldName)
        writer.value(cause)
        writer.field(countFieldName)
        writer.value(count)
        writer.field(fatalFieldName)
        writer.value(fatal)
        writer.field(lastErrorFieldName)
        writer.value(lastError)
        writer.field(serversFieldName)
        writer.startObject()
        for(kv in servers) {
            writer.field(kv.key)
            writer.startArray()
            for(o in kv.value) {
                writer.arrayField()
                o.encodeJson(writer)
            }
            writer.endArray()
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 25164
        buffer.writeVarInt(header0)
        buffer.writeString(cause)
        buffer.writeVarInt(count)
        buffer.writeVarInt(if(fatal) 1 else 0)
        buffer.writeVarLong(lastError.millis)
        buffer.writeVarLong((servers.size.toLong() shl 6) or 4 or (7 shl 3))
        for(kv in servers) {
            buffer.writeString(kv.key)
            buffer.writeVarLong((kv.value.size.toLong() shl 3) or 5)
            for(o in kv.value) {
                o.encodeBinary(buffer)
            }
        }
    }

    companion object {
        val reader = Reader(ErrorClass::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ErrorClass {
            var cause: String? = null
            var count: Int = 0
            var fatal: Boolean = false
            var lastError: DateTime? = null
            var servers: HashMap<String, ArrayList<ErrorInstance>> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        cause = buffer.readString()
                        true
                    }
                    1 -> {
                        count = buffer.readVarInt()
                        true
                    }
                    2 -> {
                        fatal = buffer.readVarInt().let {it != 0}
                        true
                    }
                    3 -> {
                        lastError = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    4 -> {
                        val length_servers = buffer.readVarLong() ushr 6
                        var i_servers = 0
                        while(i_servers < length_servers) {
                            val
                            servers_k = buffer.readString()
                            val servers_v = ArrayList<ErrorInstance>()
                            val length_servers_v = buffer.readVarLong() ushr 3
                            var i_servers_v = 0
                            while(i_servers_v < length_servers_v) {
                                servers_v!!.add(ErrorInstance.fromBinary(buffer))
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

            if(cause == null || lastError == null) {
                throw InvalidStateException("ErrorClass instance is missing required fields")
            }
            return ErrorClass(cause!!, count, fatal, lastError!!, servers)
        }

        fun fromJson(token: JsonToken): ErrorClass {
            var cause: String? = null
            var count: Int = 0
            var fatal: Boolean = false
            var lastError: DateTime? = null
            var servers: HashMap<String, ArrayList<ErrorInstance>> = HashMap()
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        causeFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            cause = token.stringPayload
                        }
                        countFieldHash -> {
                            token.expect(JsonToken.Type.NumberLit)
                            count = token.numberPayload.toInt()
                        }
                        fatalFieldHash -> {
                            token.expect(JsonToken.Type.BoolLit)
                            fatal = token.boolPayload
                        }
                        lastErrorFieldHash -> {
                            token.expect(JsonToken.Type.StringLit)
                            lastError = DateTime.parse(token.stringPayload)
                        }
                        serversFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val servers_k = token.stringPayload
                                    val servers_v = ArrayList<ErrorInstance>()
                                    token.expect(JsonToken.Type.StartArray)
                                    while(!token.peekArrayEnd()) {
                                        servers_v.add(ErrorInstance.fromJson(token))
                                    }
                                    token.expect(JsonToken.Type.EndArray)
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
            if(cause == null || lastError == null) {
                throw InvalidStateException("ErrorClass instance is missing required fields")
            }
            return ErrorClass(cause, count, fatal, lastError, servers)
        }
    }
}
