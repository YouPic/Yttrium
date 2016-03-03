package com.rimmer.yttrium.serialize

import io.netty.buffer.ByteBuf
import org.joda.time.DateTime

/**
 * Represents a type that can be serialized.
 * Currently, writable types need to support json and binary formats.
 */
interface Writable {
    /** Encodes the value as json and stores it in the provided buffer. */
    fun encodeJson(buffer: ByteBuf)

    /** Encodes the value as binary data and stores it in the provided buffer. */
    fun encodeBinary(buffer: ByteBuf)
}

/**
 * Stores a value as json.
 * The value must be either a Writable type, or any of the following builtin types:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String.
 */
fun writeJson(value: Any?, target: ByteBuf) {
    if(value is Writable) {
        value.encodeJson(target)
    } else {
        val writer = JsonWriter(target)
        when(value) {
            is Int -> writer.value(value)
            is Long -> writer.value(value)
            is String -> writer.value(value)
            is DateTime -> writer.value(value)
            is Boolean -> writer.value(value)
            is Float -> writer.value(value)
            is Double -> writer.value(value)
            is Char -> writer.value(value.toString())
            is Byte -> writer.value(value)
            is Short -> writer.value(value)
            else -> throw IllegalArgumentException("Value $value cannot be serialized.")
        }
    }
}

/**
 * Stores a value as binary data.
 * The value must be either a Writable type, or any of the following builtin types:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String.
 */
fun writeBinary(value: Any?, target: ByteBuf) {
    if(value is Writable) {
        value.encodeBinary(target)
    } else {
        val writer = BinaryWriter(target)
        when(value) {
            is Int -> writer.writeVarInt(value)
            is Long -> writer.writeVarLong(value)
            is String -> writer.writeString(value)
            is DateTime -> writer.writeVarLong(value.millis)
            is Boolean -> writer.writeVarInt(if(value) 1 else 0)
            is Float -> writer.writeFloat(value)
            is Double -> writer.writeDouble(value)
            is Char -> writer.writeString(value.toString())
            is Byte -> writer.writeVarInt(value.toInt())
            is Short -> writer.writeVarInt(value.toInt())
            else -> throw IllegalArgumentException("Value $value cannot be serialized.")
        }
    }
}