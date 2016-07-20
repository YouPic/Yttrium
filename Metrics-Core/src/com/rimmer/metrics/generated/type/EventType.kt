package com.rimmer.metrics.generated.type

import org.joda.time.DateTime
import io.netty.buffer.ByteBuf
import java.util.*
import com.rimmer.yttrium.*
import com.rimmer.yttrium.serialize.*

enum class EventType: Writable {
    Redis, MySQL, MySQLGen, MySQLProcess, Mongo, Serialize;

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeByte(ordinal)
    }

    override fun encodeJson(buffer: ByteBuf) {
        JsonWriter(buffer).value(name)
    }

    companion object {
        init {
            registerReadable(EventType::class.java, {fromJson(it)}, {fromBinary(it)})
        }

        fun fromBinary(buffer: ByteBuf): EventType {
            val index = buffer.readByte()
            val values = values()
            if(index >= values.size) throw InvalidStateException("The provided EventType enum value $index is invalid.")
            return values[index.toInt()]
        }

        fun fromJson(buffer: ByteBuf): EventType {
            val token = JsonToken(buffer)
            token.expect(JsonToken.Type.StringLit)
            try {
                return valueOf(token.stringPayload)
            } catch(e: IllegalArgumentException) {
                throw InvalidStateException("The provided EventType enum value ${token.stringPayload} is invalid.")
            }
        }
    }
}
