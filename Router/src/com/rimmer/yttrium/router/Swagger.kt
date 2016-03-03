package com.rimmer.yttrium.router

import com.rimmer.yttrium.serialize.JsonWriter
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.lang.reflect.Type
import java.util.*

class Swagger {
    class Category(val name: String) {
        val equivalences = HashMap<String, MutableList<Route>>()
    }

    class PathInfo(val path: String, val equivalencePath: String, val category: String)

    class Path(val info: PathInfo) {
        val routes = ArrayList<Route>()
    }

    class Route(val info: PathInfo, val method: HttpMethod, val version: Int) {
        val parameters = ArrayList<Parameter>()
    }

    enum class ParameterType {
        Body, Path, Query
    }

    class Parameter(val name: String, val type: ParameterType, val desc: String, val format: Type, val isOptional: Boolean)

    private val categories = ArrayList<Category>()
    private val uniqueTimes = ArrayList<Int>()
    private val definitionCache = ArrayList<ByteBuf?>()

    /** Creates a JSON swagger definition with the routes that changed between the provided version indices. */
    fun difference(oldTime: Int, newTime: Int): String {
        reset()
        formatJson(newTime, oldTime)
        return writer.toString()
    }

    /** Creates a JSON swagger definition for the provided version time. */
    fun definition(time: Int): ByteBuf {
        var index = uniqueTimes.binarySearch(time)
        if(index < 0) index = -index - 2

        if(definitionCache[index] == null) {
            reset()
            formatJson(time, null)
            val bytes = writer.toString().toByteArray()
            val buffer = Unpooled.buffer(bytes.size)
            buffer.writeBytes(bytes)
            definitionCache[index] = buffer
        }

        return definitionCache[index]!!
    }

    /** Adds a new category with the provided name. */
    fun addCategory(name: String): Category {
        val c = Category(name)
        categories.add(c)
        return c
    }

    /** Adds a route into a category created with addCategory. */
    fun addRoute(route: Route, category: Category) {
        // Add the route to the corresponding equivalence map.
        val key = route.method.name + route.info.equivalencePath
        var e = category.equivalences[key]
        if(e == null) {
            e = ArrayList<Route>()
            category.equivalences[key] = e
        }

        // Keep the list sorted.
        val index = e.binarySearchBy(route.version) {it.version}
        e.add(if(index >= 0) index else (-index - 1), route)

        // Keep the time set sorted.
        val timeIndex = uniqueTimes.binarySearchBy(route.version) {it}
        if(timeIndex < 0) {
            uniqueTimes.add(-index - 1, route.version)
            definitionCache.add(null)
        }
    }

    private val writer = Unpooled.buffer()
    private var json = JsonWriter(writer)

    private fun formatJson(time: Int, diff: Int?) {
        json.startObject()
        formatIntro(time)
        formatTags()
        formatCategories(time, diff)
        json.endObject()
    }

    private fun formatIntro(time: Int) {
        json.field("swagger").value("2.0")
        json.field("info").startObject().field("title").value("Server API - version $time").field("version").value(time.toString()).endObject()
        json.field("basePath").value("/")
        json.field("schemes").startArray().value("http").endArray()
    }

    private fun formatTags() {
        json.field("tags").startArray()
        for(c in categories) {
            json.startObject()
            json.field("name").value(c.name)
            json.endObject()
        }
        json.endArray()
    }

    private fun formatCategories(time: Int, diff: Int?) {
        json.field("paths").startObject()
        for(c in categories) {
            formatPaths(c, time, diff)
        }
        json.endObject()
    }

    private fun formatPaths(c: Category, time: Int, diff: Int?) {
        // Swagger requires us to divide everything into groups with the same path.
        // The versioning may cause some paths to become without routes, in which case they have to be removed.
        // Because of this, we lazily create a list of all paths in use before creating JSON.
        val paths = TreeMap<String, Path>()
        for(e in c.equivalences.values) {
            var candidate: Route? = null
            if(diff == null) {
                // Find the newest route in this equivalence whose version is lower than the requested time.
                candidate = e[0]
                for(r in e) {
                    if(r.version > candidate!!.version && r.version <= time) {
                        candidate = r
                    }
                }
            } else {
                // Find the newest route that was updated since the difference time.
                for(r in e) {
                    if(r.version > diff && (candidate == null || r.version > candidate.version) && r.version <= time) {
                        candidate = r
                    }
                }
            }

            // Create a path for version that was used.
            if(candidate != null) {
                var path = paths[candidate.info.path]
                if (path == null) {
                    path = Path(candidate.info)
                    paths.put(candidate.info.path, path)
                }
                path.routes.add(candidate)
            }
        }

        // Format the routes in each created path.
        for(p in paths.values) {
            json.field(p.info.path).startObject()
            formatRoutes(p, time)
            json.endObject()
        }
    }

    private fun formatRoutes(p: Path, time: Int) {
        for(r in p.routes) {
            json.field(r.method.name.toLowerCase()).startObject()
            json.field("tags").startArray().value(r.info.category).endArray()
            json.field("consumes").startArray().value("application/json").endArray()
            json.field("produces").startArray().value("application/json").endArray()
            formatParameters(r, time)
            formatResponses(r)
            json.endObject()
        }
    }

    private fun formatParameters(r: Route, time: Int) {
        json.field("parameters").startArray()
        for(p in r.parameters) {
            json.startObject()
            json.field("name").value(p.name)
            json.field("in").value(p.type.name.toLowerCase())
            json.field("required").value(!p.isOptional)
            json.field("description").value(p.desc)
            if(p.type == ParameterType.Body) {
                json.field("schema").startObject().endObject()
            } else {
                formatParameterType(p.format)
            }
            json.endObject()
        }

        // Add the version parameter.
        json.startObject()
        json.field("name").value("API-VERSION")
        json.field("in").value("header")
        json.field("required").value(true)
        json.field("default").value(time)
        json.field("description").value("The version of the api to use. This route is only available within the indicated version range.")
        json.field("type").value("integer").field("format").value("int32").field("minimum").value(r.version)
        json.field("enum").startArray().value(time).endArray()
        json.endObject()

        json.endArray()
    }

    private fun formatResponses(r: Route) {
        // TODO: Describe the route return type.
        json.field("responses").startObject()
        json.field("200").startObject().field("description").value("The request was executed successfully.").endObject()
        json.endObject()
    }

    private fun formatParameterType(type: Type) {
        when(type) {
            Int::class.java -> json.field("type").value("integer").field("format").value("int32")
            Long::class.java -> json.field("type").value("integer").field("format").value("int64")
            Float::class.java -> json.field("type").value("number").field("format").value("float")
            Double::class.java -> json.field("type").value("number").field("format").value("double")
            else -> json.field("type").value("string")
        }
    }

    private fun reset() {
        writer.resetWriterIndex()
        json = JsonWriter(writer)
    }
}