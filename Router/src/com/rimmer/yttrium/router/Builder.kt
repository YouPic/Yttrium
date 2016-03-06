package com.rimmer.yttrium.router

import java.util.*

class BuilderQuery(val name: String, val optional: Boolean, val default: Any?, val description: String)

class RouteBuilder(val router: Router, val method: HttpMethod, val path: String, val version: Int) {
    internal val queries = ArrayList<BuilderQuery>()
    internal val properties = ArrayList<RouteProperty>()

    fun property(name: String, value: Any): RouteBuilder {
        properties.add(RouteProperty(name, value))
        return this
    }

    fun arg(name: String, description: String = ""): RouteBuilder {
        queries.add(BuilderQuery(name, false, null, description))
        return this
    }

    fun optional(name: String, default: Any? = null, description: String = ""): RouteBuilder {
        queries.add(BuilderQuery(name, true, default, description))
        return this
    }

    /*
     * The following functions set a handler for this route.
     * The number of parameters determines what instance is called.
     */

    inline infix fun <reified R: Any> handle(crossinline f: RouteContext.() -> Future<R>) {
        router.addRoute(this, {f() as Future<Any>}, emptyArray(), R::class.java)
    }

    inline infix fun <reified R: Any, reified A: Any> handle(crossinline f: RouteContext.(A) -> Future<R>) {
        router.addRoute(this, {f(it[0] as A) as Future<Any>}, arrayOf(A::class.java), R::class.java)
    }

    inline infix fun <reified R: Any, reified A: Any, reified B: Any> handle(crossinline f: RouteContext.(A, B) -> Future<R>) {
        router.addRoute(this, {f(it[0] as A, it[1] as B) as Future<Any>}, arrayOf(A::class.java, B::class.java), R::class.java)
    }

    inline infix fun <reified R: Any, reified A: Any, reified B: Any, reified C: Any> handle(crossinline f: RouteContext.(A, B, C) -> Future<R>) {
        router.addRoute(this, {f(it[0] as A, it[1] as B, it[2] as C) as Future<Any>}, arrayOf(A::class.java, B::class.java, C::class.java), R::class.java)
    }

    inline infix fun <reified R: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any> handle(crossinline f: RouteContext.(A, B, C, D) -> Future<R>) {
        router.addRoute(this, {f(it[0] as A, it[1] as B, it[2] as C, it[3] as D) as Future<Any>}, arrayOf(A::class.java, B::class.java, C::class.java, D::class.java), R::class.java)
    }
}
