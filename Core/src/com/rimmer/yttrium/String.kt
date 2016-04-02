package com.rimmer.yttrium

import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

/*
 * Helper functions for handling UTF-8 strings.
 */

val emptyString = LocalByteString(ByteArray(0))
val String.utf8: ByteString get() = LocalByteString(toByteArray(Charsets.UTF_8))

interface ByteString: List<Byte> {
    override fun isEmpty() = size > 0

    /** Writes the whole string into this buffer. */
    fun write(buffer: ByteBuf)

    /** Returns an utf-16 representation of the string. This will copy the data. */
    fun utf16(): String
}

/** Contains a byte string in local jvm memory. */
class LocalByteString(private val bytes: ByteArray): ByteString {
    override val size: Int get() = bytes.size

    override fun contains(element: Byte) = bytes.contains(element)
    override fun containsAll(elements: Collection<Byte>): Boolean {
        for(e in elements) {
            if(e !in bytes) return false
        }
        return true
    }

    override fun get(index: Int) = bytes[index]
    override fun indexOf(element: Byte) = bytes.indexOf(element)
    override fun iterator() = bytes.iterator()
    override fun lastIndexOf(element: Byte) = bytes.lastIndexOf(element)
    override fun listIterator() = Iterator(bytes)
    override fun listIterator(index: Int) = Iterator(bytes, index)
    override fun subList(fromIndex: Int, toIndex: Int) = LocalByteString(bytes.copyOfRange(fromIndex, toIndex))
    override fun write(buffer: ByteBuf) {buffer.writeBytes(bytes)}
    override fun utf16() = String(bytes, Charsets.UTF_8)

    class Iterator(val bytes: ByteArray, var index: Int = 0): ListIterator<Byte> {
        override fun hasNext() = index < bytes.size
        override fun hasPrevious() = index >= 0
        override fun nextIndex() = index + 1
        override fun previousIndex() = index - 1

        override fun next(): Byte {
            val byte = bytes[index]
            index++
            return byte
        }

        override fun previous(): Byte {
            val byte = bytes[index]
            index--
            return byte
        }
    }
}

/** Contains a byte string in native memory, without copying. */
class NativeByteString(source: ByteBuffer, offset: Int, count: Int): ByteString {
    private val buffer: ByteBuffer

    init {
        buffer = source.slice()
        buffer.position(offset).limit(offset + count)
    }

    override val size: Int get() = buffer.remaining()
    override fun get(index: Int) = buffer.get(buffer.position() + index)
    
    override fun contains(element: Byte): Boolean {
        val start = buffer.position()
        while(buffer.hasRemaining()) {
            if(buffer.get() == element) {
                buffer.position(start)
                return true
            }
        }

        buffer.position(start)
        return false
    }

    override fun containsAll(elements: Collection<Byte>): Boolean {
        for(e in elements) {
            if(e !in this) return false
        }
        return true
    }

    override fun indexOf(element: Byte): Int {
        val start = buffer.position()
        while(buffer.hasRemaining()) {
            if(buffer.get() == element) {
                val found = buffer.position() - 1 - start
                buffer.position(start)
                return found
            }
        }

        buffer.position(start)
        return -1
    }

    override fun lastIndexOf(element: Byte): Int {
        val start = buffer.position()
        var found = -1
        while(buffer.hasRemaining()) {
            if(buffer.get() == element) {
                found = buffer.position() - 1 - start
            }
        }

        buffer.position(start)
        return found
    }

    override fun iterator() = It(buffer, buffer.position(), buffer.limit())
    override fun listIterator() = BiIt(buffer, buffer.position(), buffer.position(), buffer.limit())
    override fun listIterator(index: Int) = BiIt(buffer, buffer.position() + index, buffer.position(), buffer.limit())
    override fun subList(fromIndex: Int, toIndex: Int) =
        NativeByteString(buffer, buffer.position() + fromIndex, buffer.position() + toIndex)
    override fun write(buffer: ByteBuf) {buffer.writeBytes(this.buffer)}

    override fun utf16(): String {
        val start = buffer.position()
        val string = ByteArray(size)
        for(i in 0..size-1) {
            string[i] = this[i]
        }
        return String(string, Charsets.UTF_8)
    }

    class It(val bytes: ByteBuffer, var index: Int, val max: Int): Iterator<Byte> {
        override fun hasNext() = index < max
        override fun next(): Byte {
            val byte = bytes[index]
            index++
            return byte
        }
    }

    class BiIt(val bytes: ByteBuffer, var index: Int, val min: Int, val max: Int): ListIterator<Byte> {
        override fun hasNext() = index < max
        override fun hasPrevious() = index >= min
        override fun nextIndex() = index + 1
        override fun previousIndex() = index - 1

        override fun next(): Byte {
            val byte = bytes[index]
            index++
            return byte
        }

        override fun previous(): Byte {
            val byte = bytes[index]
            index--
            return byte
        }
    }
}