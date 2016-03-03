package com.rimmer.server.serialize

import io.netty.buffer.ByteBuf

/** Helper functions for writing binary data. */
class BinaryWriter(val buffer: ByteBuf) {
    fun endObject() {
        buffer.writeByte(0)
    }

    fun writeFieldId(id: Int) {
        buffer.writeByte(id)
    }

    fun writeString(s: String) {
        writeVarInt(s.length)
        buffer.writeBytes(s.toByteArray())
    }

    fun writeVarInt(value: Int) {
        var v = value
        while(v != 0) {
            if(v and 0x7f.inv() != 0) {
                buffer.writeByte(v.toInt() or 0x80)
            } else {
                buffer.writeByte(v.toInt())
            }

            v = v shr 7
        }
    }

    fun writeVarLong(value: Long) {
        var v = value
        while(v != 0L) {
            if(v and 0x7fL.inv() != 0L) {
                buffer.writeByte(v.toInt() or 0x80)
            } else {
                buffer.writeByte(v.toInt())
            }

            v = v shr 7
        }
    }

    fun writeFloat(value: Float) {
        buffer.writeFloat(value)
    }

    fun writeDouble(value: Double) {
        buffer.writeDouble(value)
    }
}