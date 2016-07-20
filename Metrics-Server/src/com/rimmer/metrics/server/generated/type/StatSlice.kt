package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.metrics.generated.type.*

data class StatSlice(
    val time: DateTime,
    val global: StatEntry,
    val paths: Map<String, StatEntry>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(timeFieldName)
        writer.value(this.time)
        writer.field(globalFieldName)
        this.global.encodeJson(writer)
        writer.field(pathsFieldName)
        writer.startObject()
        for(kv in this.paths) {
            writer.field(kv.key)
            kv.value.encodeJson(writer)
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 3400
        buffer.writeVarInt(header0)
        buffer.writeVarLong(time.millis)
        global.encodeBinary(buffer)
        buffer.writeVarLong((paths.size.toLong() shl 6) or 4 or (5 shl 3))
        for(kv in paths) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(StatSlice::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): StatSlice {
            var time: DateTime? = null
            var global: StatEntry? = null
            var paths: HashMap<String, StatEntry> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    1 -> {
                        global = StatEntry.fromBinary(buffer)
                        true
                    }
                    2 -> {
                        val length_paths = buffer.readVarLong() ushr 6
                        var i_paths = 0
                        while(i_paths < length_paths) {
                            val
                            paths_k = buffer.readString()
                            val
                            paths_v = StatEntry.fromBinary(buffer)
                            paths.put(paths_k, paths_v)
                            i_paths++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(time == null || global == null) {
                throw InvalidStateException("StatSlice instance is missing required fields")
            }
            return StatSlice(time!!, global!!, paths)
        }

        fun fromJson(token: JsonToken): StatSlice {
            var time: DateTime? = null
            var global: StatEntry? = null
            var paths: HashMap<String, StatEntry> = HashMap()
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
                        globalFieldHash -> {
                            global = StatEntry.fromJson(token)
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
                                    paths_v = StatEntry.fromJson(token)
                                    paths.put(paths_k, paths_v)
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
            if(time == null || global == null) {
                throw InvalidStateException("StatSlice instance is missing required fields")
            }
            return StatSlice(time, global, paths)
        }
    }
}
