package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.*
import io.netty.buffer.ByteBuf
import org.joda.time.DateTime
import java.util.*

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
 * This is a special wrapper around String,
 * which when stored will contain the raw string instead of being serialized to json.
 */
data class RawString(val value: String): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        buffer.writeBytes(value.toByteArray())
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeString(value)
    }

    companion object {
        init {
            registerReadable(RawString::class.java, {
                val bytes = ByteArray(it.readableBytes())
                it.readBytes(bytes)
                RawString(String(bytes))
            }, {
                RawString(it.readString())
            })
        }
    }
}

/**
 * Stores a value as json.
 * The value must be either a Writable type, or any of the following builtin types:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String, Enum.
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
            is Enum<*> -> writer.value(value.name)
            is Boolean -> writer.value(value)
            is Float -> writer.value(value)
            is Double -> writer.value(value)
            is Char -> writer.value(value.toString())
            is Byte -> writer.value(value)
            is Short -> writer.value(value)
            is Unit -> writer.startObject().endObject()
            else -> throw InvalidStateException("Value $value cannot be serialized.")
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
        when(value) {
            is Int -> target.writeVarInt(value)
            is Long -> target.writeVarLong(value)
            is String -> target.writeString(value)
            is DateTime -> target.writeVarLong(value.millis)
            is Enum<*> -> target.writeVarInt(value.ordinal)
            is Boolean -> target.writeVarInt(if(value) 1 else 0)
            is Float -> target.writeFloat(value)
            is Double -> target.writeDouble(value)
            is Char -> target.writeString(value.toString())
            is Byte -> target.writeVarInt(value.toInt())
            is Short -> target.writeVarInt(value.toInt())
            is Unit -> {}
            else -> throw InvalidStateException("Value $value cannot be serialized.")
        }
    }
}

/**
 * Reads a primitive value from a string.
 * This can be used for parsing immediate parameters such as query strings.
 * Supported target types are:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String.
 */
fun readPrimitive(source: String, target: Class<*>): Any {
    if(target == Int::class.javaObjectType) {
        return maybeParseInt(source) ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == Long::class.javaObjectType) {
        return maybeParseLong(source) ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == String::class.java) {
        return source
    } else if(target == DateTime::class.java) {
        return DateTime.parse(source)
    } else if(target.isEnum) {
        return (target as Class<Enum<*>>).enumConstants.find {
            it.name == source
        } ?: throw InvalidStateException("\"$source\" is not an instance of enum $target")
    } else if(target == Boolean::class.javaObjectType) {
        if(source == "true") return true
        else if(source == "false") return false
        else throw InvalidStateException("\"$source\" is not a boolean")
    } else if(target == Float::class.javaObjectType) {
        try {
            return java.lang.Float.parseFloat(source)
        } catch(e: Exception) {
            throw InvalidStateException("\"$source\" cannot be parsed as a number")
        }
    } else if(target == Double::class.javaObjectType) {
        try {
            return java.lang.Double.parseDouble(source)
        } catch(e: Exception) {
            throw InvalidStateException("\"$source\" cannot be parsed as a number")
        }
    } else if(target == Char::class.javaObjectType) {
        if(source.length == 1) {
            return source[0]
        } else {
            throw InvalidStateException("\"$source\" is not a character")
        }
    } else if(target == Byte::class.javaObjectType) {
        return maybeParseInt(source)?.toByte() ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == Short::class.javaObjectType) {
        return maybeParseInt(source)?.toShort() ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else {
        throw IllegalArgumentException("Target type $target is not a primitive type.")
    }
}

/**
 * Reads a value from json.
 * The value must either be registered as a Readable type, or any of the following builtin types:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String.
 */
fun readJson(buffer: ByteBuf, target: Class<*>): Any {
    val readable = readableTypes[target]
    if(readable != null) {
        return readable.fromJson(buffer)
    } else {
        val reader = JsonToken(buffer)
        if(target == Int::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toInt()
        } else if(target == Long::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toLong()
        } else if(target == String::class.java) {
            reader.expect(JsonToken.Type.StringLit)
            return reader.stringPayload
        } else if(target == DateTime::class.java) {
            reader.parse()
            if(reader.type == JsonToken.Type.NumberLit) {
                return DateTime(reader.numberPayload.toLong())
            } else if(reader.type == JsonToken.Type.StringLit) {
                return DateTime.parse(reader.stringPayload)
            } else {
                throw InvalidStateException("Expected a json date.")
            }
        } else if(target.isEnum) {
            reader.expect(JsonToken.Type.StringLit)
            return (target as Class<Enum<*>>).enumConstants.find {
                it.name == reader.stringPayload
            } ?: throw InvalidStateException("Expected instance of enum $target")
        } else if(target == Boolean::class.java) {
            reader.expect(JsonToken.Type.BoolLit)
            return reader.boolPayload
        } else if(target == Float::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toFloat()
        } else if(target == Double::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload
        } else if(target == Char::class.java) {
            reader.expect(JsonToken.Type.StringLit)
            return reader.stringPayload.firstOrNull() ?: ' '
        } else if(target == Byte::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toByte()
        } else if(target == Short::class.java) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toShort()
        } else if(target == Unit::class.javaObjectType) {
            reader.expect(JsonToken.Type.StartObject)
            reader.expect(JsonToken.Type.EndObject)
            return Unit
        } else {
            throw InvalidStateException("Value cannot be parsed into $target.")
        }
    }
}

/**
 * Reads a value from binary data.
 * The value must either be registered as a Readable type, or any of the following builtin types:
 * Boolean, Byte, Short, Int, Long, Float, Double, DateTime, Char, String.
 */
fun readBinary(buffer: ByteBuf, target: Class<*>): Any {
    val readable = readableTypes[target]
    if(readable != null) {
        return readable.fromBinary(buffer)
    } else {
        if(target == Int::class.java) {
            return buffer.readVarInt()
        } else if(target == Long::class.java) {
            return buffer.readVarLong()
        } else if(target == String::class.java) {
            return buffer.readString()
        } else if(target == DateTime::class.java) {
            return DateTime(buffer.readVarLong())
        } else if(target.isEnum) {
            val index = buffer.readVarInt()
            val values = (target as Class<Enum<*>>).enumConstants
            if(values.size <= index || index < 0) {
                throw InvalidStateException("Expected instance of enum $target")
            }
            return values[index]
        } else if(target == Boolean::class.java) {
            return if(buffer.readVarInt() == 0) false else true
        } else if(target == Float::class.java) {
            return buffer.readFloat()
        } else if(target == Double::class.java) {
            return buffer.readDouble()
        } else if(target == Char::class.java) {
            return buffer.readString().firstOrNull() ?: ' '
        } else if(target == Byte::class.java) {
            return buffer.readVarInt().toByte()
        } else if(target == Short::class.java) {
            return buffer.readVarInt().toShort()
        } else if(target == Unit::class.javaObjectType) {
            return Unit
        } else {
            throw InvalidStateException("Value cannot be parsed into $target.")
        }
    }
}

class Readable(val fromJson: (ByteBuf) -> Any, val fromBinary: (ByteBuf) -> Any)

/** Registers a readable type. */
fun registerReadable(type: Class<*>, fromJson: (ByteBuf) -> Any, fromBinary: (ByteBuf) -> Any) {
    readableTypes[type] = Readable(fromJson, fromBinary)
}

/** Contains the currently registered readable types. */
private val readableTypes = HashMap<Class<*>, Readable>()
