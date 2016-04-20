package com.rimmer.yttrium

inline fun ByteString.trim(predicate: (Byte) -> Boolean): ByteString {
    var start = 0
    var end = size - 1
    var found = false

    while(start <= end) {
        val index = if(found) end else start
        val valid = predicate(this[index])

        if(found) {
            if(valid) end -= 1
            else break
        } else {
            if(valid) start += 1
            else found = true
        }
    }

    return slice(start, end + 1)
}

inline fun ByteString.trimStart(predicate: (Byte) -> Boolean): ByteString {
    for(index in this.indices) {
        if(!predicate(this[index])) return slice(index, size)
    }
    return emptyString
}

inline fun ByteString.trimEnd(predicate: (Byte) -> Boolean): ByteString {
    for(index in this.indices.reversed()) {
        if(!predicate(this[index])) return slice(0, index + 1)
    }

    return emptyString
}

fun ByteString.trim(vararg chars: Byte): ByteString = trim { it in chars }
fun ByteString.trimStart(vararg chars: Byte): ByteString = trimStart { it in chars }
fun ByteString.trimEnd(vararg chars: Byte): ByteString = trimEnd { it in chars }
fun ByteString.trim(): ByteString = trim { it.toChar().isWhitespace() }
fun ByteString.trimStart(): ByteString = trimStart { it.toChar().isWhitespace() }
fun ByteString.trimEnd(): ByteString = trimEnd { it.toChar().isWhitespace() }
