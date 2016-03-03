package com.rimmer.server.serialize

import io.netty.buffer.ByteBuf
import org.joda.time.DateTime

/** Utility functions for generating json data. */
class JsonWriter(val buffer: ByteBuf) {
    var depth = 0
    var hasValue = 0

    fun startObject(): JsonWriter {
        buffer.writeByte('{'.toInt())
        depth++
        return this
    }

    fun endObject(): JsonWriter {
        buffer.writeByte('}'.toInt())
        depth--
        hasValue = depth
        return this
    }

    fun startArray(): JsonWriter {
        buffer.writeByte('['.toInt())
        depth++
        return this
    }

    fun endArray(): JsonWriter {
        buffer.writeByte(']'.toInt())
        depth--
        hasValue = depth
        return this
    }

    fun field(name: ByteArray): JsonWriter {
        if(hasValue < depth) {
            hasValue = depth
        } else {
            buffer.writeByte(','.toInt())
        }
        buffer.writeByte('"'.toInt())
        buffer.writeBytes(name)
        buffer.writeByte('"'.toInt())
        buffer.writeByte(':'.toInt())
        return this
    }

    fun field(name: String) = field(name.toByteArray())

    fun nullValue() = buffer.writeBytes(nullBytes)

    fun value(s: String): JsonWriter {
        buffer.writeByte('"'.toInt())
        buffer.writeBytes(s.toByteArray())
        buffer.writeByte('"'.toInt())
        return this
    }

    fun value(i: Double): JsonWriter {
        buffer.writeBytes(i.toString().toByteArray())
        return this
    }

    fun value(i: Long): JsonWriter {
        buffer.writeBytes(i.toString().toByteArray())
        return this
    }

    fun value(i: DateTime) = value(i.millis)
    fun value(i: Float) = value(i.toDouble())
    fun value(i: Int) = value(i.toLong())
    fun value(i: Short) = value(i.toLong())
    fun value(i: Byte) = value(i.toLong())
    fun value(i: Boolean) = buffer.writeBytes(if(i) trueBytes else falseBytes)

    companion object {
        val trueBytes = "true".toByteArray()
        val falseBytes = "false".toByteArray()
        val nullBytes = "null".toByteArray()
    }
}