package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

enum class MetricUnit: Writable {
    TimeUnit, ByteUnit, CountUnit, FractionUnit;

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeByte(ordinal)
    }

    override fun encodeJson(writer: JsonWriter) {
        writer.value(name)
    }


    companion object {
        val reader = Reader(MetricUnit::class.java, {fromJson(it)}, {fromBinary(it)})

        fun fromBinary(buffer: ByteBuf): MetricUnit {
            val index = buffer.readByte()
            val values = values()
            if(index >= values.size) throw InvalidStateException("The provided MetricUnit enum value $index is invalid.")
            return values[index.toInt()]
        }

        fun fromJson(token: JsonToken): MetricUnit {
            token.expect(JsonToken.Type.StringLit)
            try {
                return valueOf(token.stringPayload)
            } catch(e: IllegalArgumentException) {
                throw InvalidStateException("The provided MetricUnit enum value ${token.stringPayload} is invalid.")
            }
        }
    }
}
