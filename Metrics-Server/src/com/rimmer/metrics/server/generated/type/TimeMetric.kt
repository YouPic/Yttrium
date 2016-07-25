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
    val metric: Metric,
    val servers: Map<String, ServerMetric>
): Writable {
    override fun encodeJson(writer: JsonWriter) {
        writer.startObject()
        writer.field(timeFieldName)
        writer.value(time)
        writer.field(metricFieldName)
        metric.encodeJson(writer)
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
        val header0 = 425
        buffer.writeVarInt(header0)
        buffer.writeVarLong(time.millis)
        metric.encodeBinary(buffer)
        buffer.writeVarLong((servers.size.toLong() shl 6) or 4 or (5 shl 3))
        for(kv in servers) {
            buffer.writeString(kv.key)
            kv.value.encodeBinary(buffer)
        }
    }

    companion object {
        val reader = Reader(TimeMetric::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): TimeMetric {
            var time: DateTime? = null
            var metric: Metric? = null
            var servers: HashMap<String, ServerMetric> = HashMap()

            buffer.readObject {
                when(it) {
                    0 -> {
                        time = buffer.readVarLong().let {DateTime(it)}
                        true
                    }
                    1 -> {
                        metric = Metric.fromBinary(buffer)
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

            if(time == null || metric == null) {
                throw InvalidStateException("TimeMetric instance is missing required fields")
            }
            return TimeMetric(time!!, metric!!, servers)
        }

        fun fromJson(token: JsonToken): TimeMetric {
            var time: DateTime? = null
            var metric: Metric? = null
            var servers: HashMap<String, ServerMetric> = HashMap()
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
                        metricFieldHash -> {
                            metric = Metric.fromJson(token)
                        }
                        serversFieldHash -> {
                            token.expect(JsonToken.Type.StartObject)
                            while(true) {
                                token.parse()
                                if(token.type == JsonToken.Type.EndObject) {
                                    break
                                } else if(token.type == JsonToken.Type.FieldName) {
                                    val servers_k = token.stringPayload
                                    val
                                    servers_v = ServerMetric.fromJson(token)
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
            if(time == null || metric == null) {
                throw InvalidStateException("TimeMetric instance is missing required fields")
            }
            return TimeMetric(time, metric, servers)
        }
    }
}
