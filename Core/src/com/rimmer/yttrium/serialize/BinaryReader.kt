package com.rimmer.yttrium.serialize

import io.netty.buffer.ByteBuf

/** Helper functions for reading binary data. */
class BinaryReader(val buffer: ByteBuf) {
    fun readFieldId() = buffer.readByte().toInt()

    fun readString(): String {
        val length = readVarInt()
        val bytes = ByteArray(length)
        buffer.readBytes(bytes)
        return String(bytes)
    }

    fun readVarInt(): Int {
        var v = 0
        var c = 0
        while(c < 5) {
            val b = buffer.readUnsignedByte().toInt()
            v = v or ((b and 0x80.inv()) shl (7 * c))

            if(b and 0x80 == 0) break
            c++
        }
        return v
    }

    fun readVarLong(): Long {
        var v = 0L
        var c = 0
        while(c < 10) {
            val b = buffer.readUnsignedByte().toLong()
            v = v or ((b and 0x80.inv()) shl (7 * c))

            if(b and 0x80 == 0L) break
            c++
        }
        return v
    }

    fun readFloat(): Float {
        return buffer.readFloat()
    }

    fun readDouble(): Double {
        return buffer.readDouble()
    }
}