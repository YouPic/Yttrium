package com.rimmer.yttrium.router

import com.rimmer.yttrium.serialize.Reader

/**
 * Represents one segment in the url of a route.
 * @param name If this is a string segment, this is the parsed value.
 * For a parameter segment, this is the parameter name.
 * @param type If set, this is a parameter segment that should be parsed to this type.
 */
class PathSegment(val name: String, val type: Class<*>?, val reader: Reader?)

/** Converts the provided segments into Swagger format. */
fun buildSwaggerPath(segments: List<PathSegment>): String {
    val builder = StringBuilder()
    for(segment in segments) {
        builder.append('/')
        if(segment.reader == null) {
            builder.append(segment.name)
        } else {
            builder.append('{')
            builder.append(segment.name)
            builder.append('}')
        }
    }
    return builder.toString()
}

/** Converts the provided segments into Swagger format. */
fun buildEquivalencePath(segments: List<PathSegment>): String {
    val builder = StringBuilder()
    for(segment in segments) {
        builder.append('/')
        if(segment.reader == null) {
            builder.append(segment.name)
        } else {
            builder.append('*')
        }
    }
    return builder.toString()
}