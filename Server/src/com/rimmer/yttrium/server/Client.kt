package com.rimmer.yttrium.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
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