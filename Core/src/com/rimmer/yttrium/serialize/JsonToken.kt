package com.rimmer.yttrium.serialize

import com.rimmer.yttrium.ByteString
import com.rimmer.yttrium.ByteStringBuilder
import com.rimmer.yttrium.InvalidStateException
import com.rimmer.yttrium.emptyString
import io.netty.buffer.ByteBuf

/** Represents a json token with parsing functionality. */
class JsonToken(val buffer: ByteBuf) {
    enum class Type {
        StartObject,
        EndObject,
        StartArray,
        EndArray,
        StringLit,
        NumberLit,
        BoolLit,
        NullLit,
        FieldName
    }

    var type = Type.StartObject
    var boolPayload = false
    var numberPayload = 0.0
    var stringPayload: ByteString = emptyString

    fun expect(type: Type, allowNull: Boolean = false) {
        parse()
        if(this.type != type || (this.type == Type.NullLit && !allowNull)) {
            throw InvalidStateException("Invalid json: Expected $type")
        }
    }

    fun parse() {
        skipWhitespace()
        val b = buffer.readByte().toInt()
        if(b == '['.toInt()) {
            type = Type.StartArray
        } else if(b == ']'.toInt()) {
            type = Type.EndArray
        } else if(b == '{'.toInt()) {
            type = Type.StartObject
        } else if(b == '}'.toInt()) {
            type = Type.EndObject
        } else {
            parseValue(b.toByte())
        }
    }

    fun peekArrayEnd(): Boolean {
        return buffer.getByte(buffer.readerIndex()).toChar() == ']'
    }

    private fun parseValue(first: Byte) {
        if(first == '"'.toByte()) {
            stringPayload = parseString()
            val f = buffer.getByte(buffer.readerIndex())
            if(f == ':'.toByte()) {
                type = Type.FieldName
                buffer.skipBytes(1)
            } else {
                type = Type.StringLit
            }
        } else if(isDigit(first)) {
            type = Type.NumberLit
            numberPayload = parseFloat(first)
        } else if(first == 't'.toByte()) {
            expectChar('r'.toByte())
            expectChar('u'.toByte())
            expectChar('e'.toByte())
            type = Type.BoolLit
            boolPayload = true
        } else if(first == 'f'.toByte()) {
            expectChar('a'.toByte())
            expectChar('l'.toByte())
            expectChar('s'.toByte())
            expectChar('e'.toByte())
            type = Type.BoolLit
            boolPayload = false
        } else if(first == 'n'.toByte()) {
            expectChar('u'.toByte())
            expectChar('l'.toByte())
            expectChar('l'.toByte())
            type = Type.NullLit
        } else {
            throw InvalidStateException("Invalid json: expected a value")
        }

        val c = buffer.getByte(buffer.readerIndex())
        if(c == ','.toByte()) {
            buffer.skipBytes(1)
        }
    }

    private fun parseFloat(first: Byte): Double {
        var ch = first
        var out = 0.0

        // Check sign.
        var neg = false
        if(ch == '+'.toByte()) {
            ch = buffer.readByte()
        } else if(ch == '-'.toByte()) {
            ch = buffer.readByte()
            neg = true
        }

        // Create part before decimal point.
        while(isDigit(ch)) {
            val n = Character.digit(ch.toChar(), 10)
            out *= 10.0
            out += n
            ch = buffer.readByte()
        }

        // Check if there is a fractional part.
        if(ch == '.'.toByte()) {
            ch = buffer.readByte()
            var dec = 0.0
            var dpl = 0

            while(isDigit(ch)) {
                val n = Character.digit(ch.toChar(), 10)
                dec *= 10.0
                dec += n

                dpl++
                ch = buffer.readByte()
            }

            // We need to use a floating point power here in order to support more than 9 decimals.
            val power = Math.pow(10.0, dpl.toDouble())
            dec /= power
            out += dec
        }

        // Check if there is an exponent.
        if(ch == 'E'.toByte() || ch == 'e'.toByte()) {
            ch = buffer.readByte()

            // Check sign.
            var signNegative = false
            if(ch == '+'.toByte()) {
                ch = buffer.readByte()
            } else if(ch == '-'.toByte()) {
                ch = buffer.readByte()
                signNegative = true
            }

            // Has exp. part;
            var exp = 0.0

            while(Character.isDigit(ch.toChar())) {
                val n = Character.digit(ch.toChar(), 10)
                exp *= 10.0
                exp += n
                ch = buffer.readByte()
            }

            if(signNegative) exp = -exp;

            val power = Math.pow(10.0, exp)
            out *= power
        }

        if(neg) out = -out

        return out
    }

    private fun parseString(): ByteString {
        val string = ByteStringBuilder()
        while(true) {
            val b = buffer.readByte().toInt()
            if(b == '"'.toInt()) {
                break
            } else if(b == '\\'.toInt()) {
                parseEscaped(string)
            } else {
                string.append(b)
            }
        }
        return string.string
    }

    private fun parseEscaped(string: ByteStringBuilder) {
        val b = buffer.readByte().toInt()
        if(b == '"'.toInt()) {
            string.append('"'.toByte())
        } else if(b == '\\'.toInt()) {
            string.append('\\'.toByte())
        } else if(b == '/'.toInt()) {
            string.append('/'.toByte())
        } else if(b == 'b'.toInt()) {
            string.append('\b'.toByte())
        } else if(b == 'f'.toInt()) {
            string.append(0x0C.toByte())
        } else if(b == 'n'.toInt()) {
            string.append('\n'.toByte())
        } else if(b == 'r'.toInt()) {
            string.append('\r'.toByte())
        } else if(b == 't'.toInt()) {
            string.append('\t'.toByte())
        } else if(b == 'u'.toInt()) {
            parseUnicode(string)
        }
    }

    private fun parseUnicode(string: ByteStringBuilder) {
        val b0 = parseHexit(buffer.readByte())
        val b1 = parseHexit(buffer.readByte())
        val b2 = parseHexit(buffer.readByte())
        val b3 = parseHexit(buffer.readByte())
        val char = (b0 shl 12) or (b1 shl 8) or (b2 shl 4) or b3
        string.append("${char.toChar()}")
    }

    private fun skipWhitespace() {
        while(true) {
            val b = buffer.getByte(buffer.readerIndex()).toInt()
            if(b == 0x20 || b == 0x09 || b == 0x0A || b == 0x0D) {
                buffer.skipBytes(1)
            } else {
                break
            }
        }
    }

    private fun expectChar(c: Byte) {
        val ch = buffer.readByte()
        if(ch != c) {
            throw InvalidStateException("Invalid json: expected '$c'")
        }
    }
}

/**
 * Parses the provided character as a hexit, to an integer in the range 0..15.
 * @return The parsed number. Returns Nothing if the character is not a valid number.
 */
fun parseHexit(c: Byte): Int {
    val ch = c
    val index = ch - '0'.toByte()

    if(index < 0 || index > 54) {
        throw InvalidStateException("Invalid json: expected unicode sequence")
    }

    val res = parseHexitLookup[index]
    if(res > 15) {
        throw InvalidStateException("Invalid json: expected unicode sequence")
    }

    return res
}

// We use a small lookup table for parseHexit,
// since the number of branches would be ridiculous otherwise.
val parseHexitLookup = arrayOf(
    0,  1,  2,  3,  4,  5,  6,  7,  8,  9,	/* 0..9 */
    255,255,255,255,255,255,255,			/* :..@ */
    10, 11, 12, 13, 14, 15,					/* A..F */
    255,255,255,255,255,255,255,			/* G..` */
    255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,
    255,255,255,255,255,255,
    10, 11, 12, 13, 14, 15					/* a..f */
)

/**
 * Returns true if this is a digit.
 */
fun isDigit(c: Byte): Boolean {
    val ch = c
    val index = ch - '0'.toByte()
    return index <= 9 && index >= 0
}
