package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class ServerMetric(
    val metric: Metric,
    val paths: Map<String, Metric>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(metricFieldName)
        metric.encodeJson(writer)
        writer.field(pathsFieldName)
        writer.startObject()
        for(kv in paths) {
            writer.field(kv.key)
            kv.value.encodeJson(writer)
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 53
        buffer.writeVarInt(header0)
        metric.encodeBinary(buffer)
        buffer.writeVarLong((paths.size.toLong() shl 6) or 4 or (5 shl 3))
        for(kv in paths) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(ServerMetric::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): ServerMetric {
            var metric: Metric? = null
            var paths: HashMap<String, Metric> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        metric = Metric.fromBinary(buffer)
                        true
                    }
                    1 -> {
                        val length_paths = buffer.readVarLong() ushr 6
                        var i_paths = 0
                        while(i_paths < length_paths) {
                            val
                            paths_k = buffer.readString()
                            val
                            paths_v = Metric.fromBinary(buffer)
                            paths.put(paths_k, paths_v)
                            i_paths++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(metric == null) {
                throw InvalidStateException("ServerMetric instance is missing required fields")
            }
            return ServerMetric(metric!!, paths)
        }

        fun fromJson(token: JsonToken): ServerMetric {
            var metric: Metric? = null
            var paths: HashMap<String, Metric> = HashMap()
            token.expect(JsonToken.Type.StartObject)
            
            while(true) {
                token.parse()
                if(token.type == JsonToken.Type.EndObject) {
                    break
                } else if(token.type == JsonToken.Type.FieldName) {
                    when(token.stringPayload.hashCode()) {
                        metricFieldHash -> {
                            metric = Metric.fromJson(token)
                        }
                        pathsFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val paths_k = token.stringPayload
                                    
                                    val paths_v = Metric.fromJson(token)
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
            if(metric == null) {
                throw InvalidStateException("ServerMetric instance is missing required fields")
            }
            return ServerMetric(metric, paths)
        }
    }
}
