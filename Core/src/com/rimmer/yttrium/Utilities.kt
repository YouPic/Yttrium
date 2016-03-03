package com.rimmer.yttrium

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
