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
data class RawString(val value: ByteString): Writable {
    override fun encodeJson(buffer: ByteBuf) {
        value.write(buffer)
    }

    override fun encodeBinary(buffer: ByteBuf) {
        buffer.writeByteString(value)
    }

    companion object {
        init {
            registerReadable(RawString::class.java, {
                val bytes = ByteArray(it.readableBytes())
                it.readBytes(bytes)
                RawString(LocalByteString(bytes))
            }, {
                RawString(it.readByteString())
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
            is ByteString -> writer.value(value)
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
            is Collection<*> -> {
                writer.startArray()
                for(i in value) {
                    writer.arrayField()
                    writeJson(i, target)
                }
                writer.endArray()
            }
            is Map<*, *> -> {
                writer.startObject()
                for(kv in value) {
                    val k = kv.key
                    when(k) {
                        is String -> writer.field(k)
                        is ByteArray -> writer.field(k)
                        else -> throw IllegalArgumentException("JSON map keys must be String or ByteArray")
                    }
                    writeJson(kv.value, target)
                }
                writer.endObject()
            }
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
        when(value) {
            is Int -> target.writeVarInt(value)
            is Long -> target.writeVarLong(value)
            is ByteString -> target.writeByteString(value)
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
            is Collection<*> -> {
                target.writeVarInt(value.size)
                for(i in value) writeBinary(i, target)
            }
            is Map<*, *> -> {
                target.writeVarInt(value.size)
                for(kv in value) {
                    writeBinary(kv.key, target)
                    writeBinary(kv.value, target)
                }
            }
            else -> throw IllegalArgumentException("Value $value cannot be serialized.")
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
    if(target == Int::class.javaObjectType || target == Int::class.javaPrimitiveType) {
        return maybeParseInt(source) ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == Long::class.javaObjectType || target == Long::class.javaPrimitiveType) {
        return maybeParseLong(source) ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == ByteString::class.java) {
        return source.utf8
    } else if(target == String::class.java) {
        return source
    } else if(target == Unit::class.javaObjectType || target == Unit::class.javaPrimitiveType) {
        return Unit
    } else if(target == DateTime::class.java) {
        return DateTime.parse(source)
    } else if(target.isEnum) {
        return (target as Class<Enum<*>>).enumConstants.find {
            it.name == source
        } ?: throw InvalidStateException("\"$source\" is not an instance of enum $target")
    } else if(target == Boolean::class.javaObjectType || target == Boolean::class.javaPrimitiveType) {
        if(source == "true") return true
        else if(source == "false") return false
        else throw InvalidStateException("\"$source\" is not a boolean")
    } else if(target == Float::class.javaObjectType || target == Float::class.javaPrimitiveType) {
        try {
            return java.lang.Float.parseFloat(source)
        } catch(e: Exception) {
            throw InvalidStateException("\"$source\" cannot be parsed as a number")
        }
    } else if(target == Double::class.javaObjectType || target == Double::class.javaPrimitiveType) {
        try {
            return java.lang.Double.parseDouble(source)
        } catch(e: Exception) {
            throw InvalidStateException("\"$source\" cannot be parsed as a number")
        }
    } else if(target == Char::class.javaObjectType || target == Char::class.javaPrimitiveType) {
        if(source.length == 1) {
            return source[0]
        } else {
            throw InvalidStateException("\"$source\" is not a character")
        }
    } else if(target == Byte::class.javaObjectType || target == Byte::class.javaPrimitiveType) {
        return maybeParseInt(source)?.toByte() ?: throw InvalidStateException("\"$source\" cannot be parsed as an integer")
    } else if(target == Short::class.javaObjectType || target == Short::class.javaPrimitiveType) {
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
        if(target == Int::class.javaObjectType || target == Int::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toInt()
        } else if(target == Long::class.javaObjectType || target == Long::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toLong()
        } else if(target == ByteString::class.java) {
            reader.expect(JsonToken.Type.StringLit)
            return reader.stringPayload
        } else if(target == String::class.java) {
            reader.expect(JsonToken.Type.StringLit)
            return reader.stringPayload.utf16()
        } else if(target == Unit::class.javaObjectType || target == Unit::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.StartObject)
            reader.expect(JsonToken.Type.EndObject)
            return Unit
        } else if(target == DateTime::class.java) {
            reader.parse()
            if(reader.type == JsonToken.Type.NumberLit) {
                return DateTime(reader.numberPayload.toLong())
            } else if(reader.type == JsonToken.Type.StringLit) {
                return DateTime.parse(reader.stringPayload.utf16())
            } else {
                throw InvalidStateException("Expected a json date.")
            }
        } else if(target.isEnum) {
            reader.expect(JsonToken.Type.StringLit)
            return (target as Class<Enum<*>>).enumConstants.find {
                it.name.utf8 == reader.stringPayload
            } ?: throw InvalidStateException("Expected instance of enum $target")
        } else if(target == Boolean::class.javaObjectType || target == Boolean::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.BoolLit)
            return reader.boolPayload
        } else if(target == Float::class.javaObjectType || target == Float::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toFloat()
        } else if(target == Double::class.javaObjectType || target == Double::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload
        } else if(target == Char::class.javaObjectType || target == Char::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.StringLit)
            return reader.stringPayload.firstOrNull() ?: ' '
        } else if(target == Byte::class.javaObjectType || target == Byte::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toByte()
        } else if(target == Short::class.javaObjectType || target == Short::class.javaPrimitiveType) {
            reader.expect(JsonToken.Type.NumberLit)
            return reader.numberPayload.toShort()
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
        if(target == Int::class.javaObjectType || target == Int::class.javaPrimitiveType) {
            return buffer.readVarInt()
        } else if(target == Long::class.javaObjectType || target == Long::class.javaPrimitiveType) {
            return buffer.readVarLong()
        } else if(target == ByteString::class.javaObjectType) {
            return buffer.readByteString()
        } else if(target == String::class.javaObjectType) {
            return buffer.readString()
        } else if(target == Unit::class.javaObjectType || target == Unit::class.javaPrimitiveType) {
            return Unit
        } else if(target == DateTime::class.javaObjectType) {
            return DateTime(buffer.readVarLong())
        } else if(target.isEnum) {
            val index = buffer.readVarInt()
            val values = (target as Class<Enum<*>>).enumConstants
            if(values.size <= index || index < 0) {
                throw InvalidStateException("Expected instance of enum $target")
            }
            return values[index]
        } else if(target == Boolean::class.javaObjectType || target == Boolean::class.javaPrimitiveType) {
            return if(buffer.readVarInt() == 0) false else true
        } else if(target == Float::class.javaObjectType || target == Float::class.javaPrimitiveType) {
            return buffer.readFloat()
        } else if(target == Double::class.javaObjectType || target == Double::class.javaPrimitiveType) {
            return buffer.readDouble()
        } else if(target == Char::class.javaObjectType || target == Char::class.javaPrimitiveType) {
            return buffer.readString().firstOrNull() ?: ' '
        } else if(target == Byte::class.javaObjectType || target == Byte::class.javaPrimitiveType) {
            return buffer.readVarInt().toByte()
        } else if(target == Short::class.javaObjectType || target == Short::class.javaPrimitiveType) {
            return buffer.readVarInt().toShort()
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
