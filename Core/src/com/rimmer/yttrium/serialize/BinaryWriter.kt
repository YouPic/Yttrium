package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.ByteString
import io.netty.buffer.ByteBuf

/*
 * Helper functions for writing binary data.
 */

fun ByteBuf.endObject() {
    writeByte(0)
}

fun ByteBuf.writeFieldId(id: Int) {
    writeByte(id)
}

fun ByteBuf.writeByteString(s: ByteString) {
    writeVarInt(s.size)
    s.write(this)
}

fun ByteBuf.writeString(s: String) {
    writeVarInt(s.length)
    writeBytes(s.toByteArray(Charsets.UTF_8))
}

fun ByteBuf.writeVarInt(value: Int) {
    var v = value
    do {
        if(v and 0x7f.inv() != 0) {
            writeByte(v.toInt() or 0x80)
        } else {
            writeByte(v.toInt())
        }

        v = v ushr 7
    } while(v != 0)
}

fun ByteBuf.writeVarLong(value: Long) {
    var v = value

    do {
        if(v and 0x7fL.inv() != 0L) {
            writeByte(v.toInt() or 0x80)
        } else {
            writeByte(v.toInt())
        }

        v = v ushr 7
    } while(v != 0L)
}