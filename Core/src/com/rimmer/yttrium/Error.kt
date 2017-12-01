package com.rimmer.yttrium

/*
 * Contains generic error types used within the server.
 * These errors are caught by the router and transformed into http responses.
 */

open class RouteException(text: String, val response: Any?): Exception(text) {
    override fun toString(): String {
        return "${javaClass.simpleName}: $message"
    }
}

/** This is mapped to 404 and should be thrown whenever something that was requested doesn't exist. */
open class NotFoundException(response: Any? = null) : RouteException("not_found", response)

/** This is mapped to 403 and should be thrown whenever a session has insufficient permissions for an operation. */
open class UnauthorizedException(text: String = "invalid_token", response: Any? = null) : RouteException(text, response)

/** This is mapped to 400 and should be thrown whenever a request tries to do something that's impossible in that context. */
open class InvalidStateException(cause: String, response: Any? = null): RouteException(cause, response)

/** This is mapped to the provided http code and should be thrown for errors that don't fit any other exception. */
open class HttpException(val errorCode: Int, cause: String, response: Any? = null): RouteException(cause, response)
