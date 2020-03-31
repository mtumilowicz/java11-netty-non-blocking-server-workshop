package workshop;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

class ClientWorkshop {

    // set InetSocketAddress, hint: new InetSocketAddress(host, port)
    private final InetSocketAddress socketAddress = null;

    public static void main(String[] args) throws Exception {
        new ClientWorkshop().start();
    }

    void start() throws Exception {
        // create EventLoopGroup, hint: NioEventLoopGroup
        try {
            // create bootstrap to create and connect new client channel, hint: new Bootstrap()
            // set EventLoopGroup that provides EventLoops for processing Channel events, hint: bootstrap.group()
            // set channel implementation, hint: NioSocketChannel
            // set remote address, hint: remoteAddress
            // set handler, hint: ChannelHandler

            // connect synchronously, hint: bootstrap.connect(), sync()
            // wait synchronously for closing: hint: connection.channel(), closeFuture(), sync()
        } finally {
            // close the event group synchronously, hint: group.shutdownGracefully(), sync()
        }
    }

    private static class ChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel channel) {
            // initialize each new Channel with an handler instance
            // hint: channel.pipeline(), addLast, ClientMessageHandlerWorkshop
        }
    }
}