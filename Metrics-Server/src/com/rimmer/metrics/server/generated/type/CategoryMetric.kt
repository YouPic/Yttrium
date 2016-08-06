package com.rimmer.metrics.server.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*
import com.rimmer.yttrium.router.plugin.IPAddress
import com.rimmer.metrics.generated.type.*

data class CategoryMetric(
    val metric: Metric,
    val unit: MetricUnit,
    val servers: Map<String, ServerMetric>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(metricFieldName)
        metric.encodeJson(writer)
        writer.field(unitFieldName)
        unit.encodeJson(writer)
        writer.field(serversFieldName)
        writer.startObject()
        for(kv in servers) {
            writer.field(kv.key)
            kv.value.encodeJson(writer)
        }
        writer.endObject()
        writer.endObject()
    }

    override fun encodeBinary(buffer: ByteBuf) {
        val header0 = 397
        buffer.writeVarInt(header0)
        metric.encodeBinary(buffer)
        unit.encodeBinary(buffer)
        buffer.writeVarLong((servers.size.toLong() shl 6) or 4 or (5 shl 3))
        for(kv in servers) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(CategoryMetric::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): CategoryMetric {
            var metric: Metric? = null
            var unit: MetricUnit? = null
            var servers: HashMap<String, ServerMetric> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        metric = Metric.fromBinary(buffer)
                        true
                    }
                    1 -> {
                        unit = MetricUnit.fromBinary(buffer)
                        true
                    }
                    2 -> {
                        val length_servers = buffer.readVarLong() ushr 6
                        var i_servers = 0
                        while(i_servers < length_servers) {
                            val
                            servers_k = buffer.readString()
                            val
                            servers_v = ServerMetric.fromBinary(buffer)
                            servers.put(servers_k, servers_v)
                            i_servers++
                        }
                        true
                    }
                    else -> false
                }
            }

            if(metric == null || unit == null) {
                throw InvalidStateException("CategoryMetric instance is missing required fields")
            }
            return CategoryMetric(metric!!, unit!!, servers)
        }

        fun fromJson(token: JsonToken): CategoryMetric {
            var metric: Metric? = null
            var unit: MetricUnit? = null
            var servers: HashMap<String, ServerMetric> = HashMap()
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
                        unitFieldHash -> {
                            unit = MetricUnit.fromJson(token)
                        }
                        serversFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val servers_k = token.stringPayload
                                    
                                    val servers_v = ServerMetric.fromJson(token)
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
            if(metric == null || unit == null) {
                throw InvalidStateException("CategoryMetric instance is missing required fields")
            }
            return CategoryMetric(metric, unit, servers)
        }
    }
}
