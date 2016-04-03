package com.rimmer.yttrium

import io.netty.channel.EventLoop

/**
 * Represents a base context that can be sent around between asynchronous functions.
 * This allows the function to get state needed for connection pools, etc.
 * @param eventLoop The event loop we are currently in.
 * This facilitates the creation of thread-local connection pools without synchronization.
 * @param id A generic context id that can be used to identify the source of a function call.
 */
open class Context(val eventLoop: EventLoop, val id: Long)
