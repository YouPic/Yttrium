package com.rimmer.yttrium.mysql

import com.rimmer.mysql.dsl.Column
import com.rimmer.mysql.dsl.Table
import com.rimmer.mysql.protocol.CodecExtender
import com.rimmer.mysql.protocol.constants.Type
import com.rimmer.mysql.protocol.decoder.readLengthEncoded
import com.rimmer.mysql.protocol.decoder.unknownTarget
import com.rimmer.mysql.protocol.decoder.writeLengthEncoded
import com.rimmer.yttrium.ByteString
import com.rimmer.yttrium.LocalByteString
import io.netty.buffer.ByteBuf

fun Table.byteText(name: String): Column<ByteString> {
    val answer = Column(this, name, ByteString::class.javaObjectType)
    columns.add(answer)
    return answer
}

object Codec: CodecExtender {
    override fun encode(buffer: ByteBuf, types: ByteArray, index: Int, value: Any) {
        if(value is ByteString) {
            types[index * 2] = Type.VARCHAR.toByte()
            buffer.writeLengthEncoded(value.size)
            value.write(buffer)
        }
    }

    override fun decodeString(buffer: ByteBuf, targetType: Class<*>?): Any {
        if(targetType === ByteString::class.java) {
            val length = buffer.readLengthEncoded().toInt()
            val bytes = ByteArray(length)
            buffer.readBytes(bytes)
            return LocalByteString(bytes)
        } else throw unknownTarget(targetType)
    }
}