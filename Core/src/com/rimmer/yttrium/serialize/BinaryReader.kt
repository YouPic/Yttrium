package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.ByteString
import com.rimmer.yttrium.LocalByteString
import io.netty.buffer.ByteBuf

/*
 * Helper functions for reading binary data.
 */

fun ByteBuf.readFieldId() = readByte().toInt()

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