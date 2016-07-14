package com.rimmer.yttrium

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/*
 * Misc. helper functions that don't belong in a specific place.
 */

/** Calculates the hash of a subset of this string. */
fun String.sliceHash(start: Int, end: Int): Int {
    var h = 0
    var i = start
    while(i < end) {
        h = 31 * h + this[i].toInt()
        i++
    }
    return h
}

/** Tries to parse a Long from a string. Returns null on failure. */
fun maybeParseLong(text: String): Long? {
    try {
        return java.lang.Long.parseLong(text)
    } catch(e: NumberFormatException) {
        return null
    }
}

/** Tries to parse a Long from a string. Returns the provided default on failure. */
fun parseLong(text: String, otherwise: Long = 0L) = maybeParseLong(text) ?: otherwise

/** Tries to parse an Int from a string. Returns null on failure. */
fun maybeParseInt(text: String): Int? {
    try {
        return java.lang.Integer.parseInt(text)
    } catch(e: NumberFormatException) {
        return null
    }
}

/** Tries to parse an Int from a string. Returns the provided default on failure. */
fun parseInt(text: String, otherwise: Int = 0) = maybeParseInt(text) ?: otherwise

/** Iterates over the data structure, but calls a different function for the last element. */
inline fun <E> Iterable<E>.iterateLast(element: (E) -> Unit, last: (E) -> Unit) {
    val i = iterator()
    if(i.hasNext()) {
        while(true) {
            val e = i.next()
            if(i.hasNext()) {
                element(e)
            } else {
                last(e)
                break
            }
        }
    }
}

/** Writes elements to a string separated by sep. */
inline fun <E> Iterable<E>.sepBy(string: StringBuilder, sep: String, f: (E) -> Unit) {
    iterateLast({f(it); string.append(sep)}, {f(it)})
}

/** Converts a byte buffer to a string. */
val ByteBuf.string: String get() = toString(Charsets.UTF_8)

/** Converts a string to a bytebuf. */
val String.byteBuf: ByteBuf get() = Unpooled.wrappedBuffer(toByteArray(Charsets.UTF_8))
