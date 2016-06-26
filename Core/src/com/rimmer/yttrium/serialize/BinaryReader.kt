package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.ByteString
import com.rimmer.yttrium.LocalByteString
import io.netty.buffer.ByteBuf

object FieldType {
    val VarInt = 0
    val Fixed32 = 1
    val Fixed64 = 2
    val LengthEncoded = 3
    val Object = 4
    val Unit = 5
}

/*
 * Helper functions for reading binary data.
 */

fun ByteBuf.readFieldId() = readVarInt()
fun typeFromId(id: Int) = id and 0b111
fun fieldFromId(id: Int) = id shr 3

fun ByteBuf.readString(): String {
    val length = readVarInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes)
}

fun ByteBuf.readByteString(): ByteString {
    val length = readVarInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return LocalByteString(bytes)
}

fun ByteBuf.readBinary(): ByteBuf {
    val length = readVarInt()
    return readBytes(length)
}

fun ByteBuf.readVarInt(): Int {
    var v = 0
    var c = 0
    while(c < 5) {
        val b = readUnsignedByte().toInt()
        v = v or ((b and 0x80.inv()) shl (7 * c))

        if(b and 0x80 == 0) break
        c++
    }
    return v
}

fun ByteBuf.readVarLong(): Long {
    var v = 0L
    var c = 0
    while(c < 10) {
        val b = readUnsignedByte().toLong()
        v = v or ((b and 0x80.inv()) shl (7 * c))

        if(b and 0x80 == 0L) break
        c++
    }
    return v
}

fun ByteBuf.skipField(id: Int) {
    val type = typeFromId(id)
    if(type == FieldType.Object) {
        skipObject(type)
    } else {
        skipValue(type)
    }
}

private fun ByteBuf.skipObject(type: Int) {
    skipValue(type)
    while(true) {
        val field = readFieldId()
        if(fieldFromId(field) == 0) break
        else skipField(field)
    }
}

private fun ByteBuf.skipValue(type: Int) {
    when(type) {
        FieldType.VarInt -> readVarLong()
        FieldType.Fixed32 -> skipBytes(4)
        FieldType.Fixed64 -> skipBytes(8)
        FieldType.LengthEncoded -> {
            val length = readVarInt()
            skipBytes(length)
        }
        FieldType.Unit -> {}
    }
}