package com.rimmer.yttrium.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

/** Connects to a remote server as a client. */
inline fun connect(
    context: ServerContext,
    host: String,
    port: Int,
    timeout: Int = 0,
    crossinline pipeline: ChannelPipeline.() -> Unit,
    crossinline onFail: (Throwable?) -> Unit
): ChannelFuture {
    val channel = if(context.handlerGroup is EpollEventLoopGroup) {
        EpollSocketChannel::class.java
    } else {
        NioSocketChannel::class.java
    }

    val init = object: ChannelInitializer<SocketChannel>() {
        override fun initChannel(channel: SocketChannel) { pipeline(channel.pipeline()) }
    }

    val b = Bootstrap().group(context.handlerGroup).channel(channel).handler(init)
    if(timeout > 0) b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)

    val promise = b.connect(host, port)
    promise.addListener {
        if(!it.isSuccess) { onFail(it.cause()) }
    }
    return promise
}

/**
 * Runs a client by creating a server context and returning it.
 * @param threadCount The number of threads to use. If 0, one thread is created for each cpu.
 * @param useNative Use native transport instead of NIO if possible (Linux only).
 */
fun runClient(threadCount: Int = 0, useNative: Boolean = false): ServerContext {
    // Create the server thread pools to use for every module.
    // Use native Epoll if possible, since it gives much better performance for small packets.
    val handlerThreads = if(threadCount == 0) Runtime.getRuntime().availableProcessors() else threadCount
    var acceptorGroup: EventLoopGroup
    var handlerGroup: EventLoopGroup

    if(Epoll.isAvailable() && useNative) {
        acceptorGroup = EpollEventLoopGroup(1)
        handlerGroup = EpollEventLoopGroup(handlerThreads)
    } else {
        acceptorGroup = NioEventLoopGroup(1)
        handlerGroup = NioEventLoopGroup(handlerThreads)
    }

    return ServerContext(acceptorGroup, handlerGroup)
}