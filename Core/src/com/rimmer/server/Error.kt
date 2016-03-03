package com.rimmer.server

/*
 * Contains generic error types used within the server.
 * These errors are caught by the router and transformed into http responses.
 */

/** This is mapped to 404 and should be thrown whenever something that was requested doesn't exist. */
class NotFoundException : Exception("not_found")

/** This is mapped to 403 and should be thrown whenever a session has insufficient permissions for an operation. */
class UnauthorizedException : Exception("no_permission")

/** This is mapped to 400 and should be thrown whenever a request tries to do something that's impossible in that context. */
class InvalidStateException(cause: String): Exception(cause)