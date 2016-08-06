package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class TimeMetric(
    val time: DateTime,
    val categories: Map<String, CategoryMetric>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(categoriesFieldName)
        writer.startObject()
        for(kv in categories) {
            writer.field(kv.key)
            kv.value.encodeJson(writer)
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 49
        buffer.writeVarInt(header0)
        buffer.writeVarLong(time.millis)
        buffer.writeVarLong((categories.size.toLong() shl 6) or 4 or (5 shl 3))
        for(kv in categories) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(TimeMetric::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): TimeMetric {
            var time: DateTime? = null
            var categories: HashMap<String, CategoryMetric> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    1 -> {
                        val length_categories = buffer.readVarLong() ushr 6
                        var i_categories = 0
                        while(i_categories < length_categories) {
                            val
                            categories_k = buffer.readString()
                            val
                            categories_v = CategoryMetric.fromBinary(buffer)
                            categories.put(categories_k, categories_v)
                            i_categories++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(time == null) {
                throw InvalidStateException("TimeMetric instance is missing required fields")
            }
            return TimeMetric(time!!, categories)
        }

        fun fromJson(token: JsonToken): TimeMetric {
            var time: DateTime? = null
            var categories: HashMap<String, CategoryMetric> = HashMap()
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
                        categoriesFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val categories_k = token.stringPayload
                                    
                                    val categories_v = CategoryMetric.fromJson(token)
                                    categories.put(categories_k, categories_v)
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
                throw InvalidStateException("TimeMetric instance is missing required fields")
            }
            return TimeMetric(time, categories)
        }
    }
}
