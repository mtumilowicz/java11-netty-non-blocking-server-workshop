package workshop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

class ServerWorkshop {

    // set InetSocketAddress, hint: new InetSocketAddress(host, port)
    private InetSocketAddress socketAddress = null;

    public static void main(String[] args) throws Exception {
        new ServerWorkshop().start();
    }

    void start() throws Exception {
        // create EventLoopGroup, hint: NioEventLoopGroup
        try {
            // create bootstrap to create and connect new client channel, hint: new ServerBootstrap()
            // set EventLoopGroup that provides EventLoops for processing Channel events, hint: bootstrap.group()
            // set channel implementation, hint: NioSocketChannel
            // set local address, hint: localAddress
            // set a ChannelInboundHandler for I/O and data for the accepted channels, hint: childHandler, ChildChannelHandler

            // bind synchronously, hint: bootstrap.bind(), sync()
            // wait synchronously for closing: hint: binding.channel(), closeFuture(), sync()
        } finally {
            // close the event group synchronously, hint: group.shutdownGracefully(), sync()
        }
    }

    private static class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel channel) {
            // initialize each new Channel with an handler instance
            // hint: channel.pipeline(), addLast, ServerMessageHandlerWorkshop
        }
    }
}