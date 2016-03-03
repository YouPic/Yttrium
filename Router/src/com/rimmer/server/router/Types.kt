package com.rimmer.server.router

/**
 * Defines an http GET path. The path string can contain parameters prepended by ':'.
 * Each function parameter that has no corresponding path parameter is bound as a query parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Get(val path: String)

/**
 * Defines an http POST path. The path string can contain parameters prepended by ':'.
 * Each function parameter that has no corresponding path parameter is bound as a query parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Post(val path: String)

/**
 * Defines an http PUT path. The path string can contain parameters prepended by ':'.
 * Each function parameter that has no corresponding path parameter is bound as a query parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Put(val path: String)

/**
 * Defines an http DELETE path. The path string can contain parameters prepended by ':'.
 * Each function parameter that has no corresponding path parameter is bound as a query parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Delete(val path: String)