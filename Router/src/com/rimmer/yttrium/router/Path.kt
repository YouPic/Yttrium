package com.rimmer.yttrium.router

import com.rimmer.yttrium.serialize.Reader

/**
 * Represents one segment in the url of a route.
 * @param name If this is a string segment, this is the parsed value.
 * For a parameter segment, this is the parameter name.
 * @param type If set, this is a parameter segment that should be parsed to this type.
 */
class PathSegment(val name: String, val reader: Reader?)

/**
 * Compares the segments in both paths and returns true
 * if the paths would be equivalent when matched by a route parser.
 */
fun equivalent(lhs: List<PathSegment>, rhs: List<PathSegment>): Boolean {
    if(lhs.size != rhs.size) return false

    val l = lhs.iterator()
    val r = rhs.iterator()
    while(l.hasNext()) {
        val ls = l.next()
        val rs = r.next()
        if(ls.name != rs.name && ls.reader == null && rs.reader == null) return false
    }
    return true
}

/**
 * Creates a list of segments from the provided path string.
 * @param path The path string, in the format "path/to/:param1/resource/:param2"
 * @param targetReaders A list of target readers of the route handler.
 * This will use as many types as there are parameters in the path; the remaining ones are ignored.
 */
fun buildSegments(path: String, targetReaders: Array<Reader>): List<PathSegment> {
    var segments = path.split('/')

    // Filter out any leading slash in the path.
    if(segments.size > 0 && segments[0] == "") {
        segments = segments.drop(1)
    }

    // Create the path segments.
    var parameterIndex = 0
    return segments.map { s ->
        if(s.startsWith(':')) {
            if(targetReaders.size <= parameterIndex) {
                throw IllegalArgumentException("The path contains more parameters than there are target types.")
            }
            PathSegment(s.drop(1), targetReaders[parameterIndex++])
        } else {
            PathSegment(s, null)
        }
    }
}

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