package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.*
import io.netty.buffer.ByteBuf
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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

class Writer<T>(val toJson: JsonWriter.(T) -> Unit, val toBinary: ByteBuf.(T) -> Unit)
class Reader(val target: Class<*>, val fromJson: (JsonToken) -> Any, val fromBinary: (ByteBuf) -> Any)

val intWriter = Writer<Int>({ value(it) }, { writeVarInt(it) })
val longWriter = Writer<Long>({ value(it) }, { writeVarLong(it) })
val byteStringWriter = Writer<ByteString>({ value(it) }, { writeByteString(it) })
val stringWriter = Writer<String>({ value(it) }, { writeString(it) })
val dateTimeWriter = Writer<DateTime>({ value(it) }, { writeVarLong(it.millis) })
val dateWriter = Writer<Date>({ value(it.time) }, { writeVarLong(it.time) })
val enumWriter = Writer<Enum<*>>({ value(it.name) }, { writeVarInt(it.ordinal) })
val booleanWriter = Writer<Boolean>({ value(it) }, { writeBoolean(it) })
val floatWriter = Writer<Float>({ value(it) }, { writeFloat(it) })
val doubleWriter = Writer<Double>({ value(it) }, { writeDouble(it) })
val charWriter = Writer<Char>({ value(it.toString()) }, { writeString(it.toString()) })
val byteWriter = Writer<Byte>({ value(it) }, { writeVarInt(it.toInt()) })
val shortWriter = Writer<Short>({ value(it) }, { writeVarInt(it.toInt()) })
val unitWriter = Writer<Unit>({ startObject().endObject() }, {})

val intReader = Reader(Int::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload.toInt()
}, {
    it.readVarInt()
})

val longReader = Reader(Long::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload.toLong()
}, {
    it.readVarLong()
})

val shortReader = Reader(Short::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload.toShort()
}, {
    it.readVarInt().toShort()
})

val byteReader = Reader(Byte::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload.toByte()
}, {
    it.readVarInt().toByte()
})

val charReader = Reader(Char::class.javaObjectType, {
    it.expect(JsonToken.Type.StringLit)
    it.stringPayload.first()
}, {
    it.readString().first()
})

val unitReader = Reader(Unit::class.javaObjectType, {
    it.expect(JsonToken.Type.StartObject)
    it.expect(JsonToken.Type.EndObject)
}, {})

val floatReader = Reader(Float::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload.toFloat()
}, {
    it.readFloat()
})

val doubleReader = Reader(Double::class.javaObjectType, {
    it.expect(JsonToken.Type.NumberLit)
    it.numberPayload
}, {
    it.readDouble()
})

val booleanReader = Reader(Boolean::class.javaObjectType, {
    it.expect(JsonToken.Type.BoolLit)
    it.boolPayload
}, {
    it.readBoolean()
})

val stringReader = Reader(String::class.java, {
    val byte = it.useByteString
    it.useByteString = false
    it.expect(JsonToken.Type.StringLit)
    it.useByteString = byte
    it.stringPayload
}, {
    it.readString()
})

val byteStringReader = Reader(ByteString::class.java, {
    val byte = it.useByteString
    it.useByteString = true
    it.expect(JsonToken.Type.StringLit)
    it.useByteString = byte
    it.byteStringPayload
}, {
    it.readByteString()
})

val dateTimeReader = Reader(DateTime::class.java, {
    it.parse()
    if(it.type == JsonToken.Type.NumberLit) {
        DateTime(it.numberPayload.toLong(), DateTimeZone.UTC)
    } else if(it.type == JsonToken.Type.StringLit) {
        DateTime.parse(it.stringPayload)
    } else {
        throw InvalidStateException("Expected a json date.")
    }
}, {
    DateTime(it.readVarLong(), DateTimeZone.UTC)
})

val dateReader = Reader(Date::class.java, {
    it.parse()
    if(it.type == JsonToken.Type.NumberLit) {
        Date(it.numberPayload.toLong())
    } else if(it.type == JsonToken.Type.StringLit) {
        Date(DateTime.parse(it.stringPayload).millis)
    } else {
        throw InvalidStateException("Expected a json date.")
    }
}, {
    Date(it.readVarLong())
})

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
        val reader = Reader(RawString::class.java, {
            val bytes = ByteArray(it.buffer.readableBytes())
            it.buffer.readBytes(bytes)
            RawString(String(bytes))
        }, {
            RawString(it.readString())
        })
    }
}

/**
 * This is a special wrapper around ByteBuf,
 * which represents a parameter that receives the body of a request.
 */
data class BodyContent(val value: ByteBuf)

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
            null -> writer.nullValue()
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
    } else if(target == String::class.java) {
        return source
    } else if(target == ByteString::class.java) {
        return source.utf8
    } else if(target == Unit::class.javaObjectType || target == Unit::class.javaPrimitiveType) {
        return Unit
    } else if(target == DateTime::class.java) {
        return maybeParseLong(source)?.let { DateTime(it, DateTimeZone.UTC) } ?: DateTime.parse(source)
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

/** Helper function for parsing auto-generated data types. */
inline fun parseJsonObject(buffer: ByteBuf, field: (JsonToken) -> Boolean) {
    val token = JsonToken(buffer)
    token.expect(JsonToken.Type.StartObject)

    while(true) {
        token.parse()
        if(token.type == JsonToken.Type.EndObject) {
            break
        } else if(token.type == JsonToken.Type.FieldName) {
            if(!field(token)) {
                token.skipValue()
            }
        } else {
            throw InvalidStateException("Invalid json: expected field or object end")
        }
    }
}
