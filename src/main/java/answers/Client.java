package answers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

class Client {

    private final InetSocketAddress socketAddress = new InetSocketAddress("localhost", 8080);

    public static void main(String[] args) throws Exception {
        new Client().start();
    }

    void start() throws Exception {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(socketAddress)
                    .handler(new ChannelHandler());
            ChannelFuture connection = bootstrap.connect().sync();
            connection.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }

    private static class ChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) { // initialize each new Channel with an EchoServerHandler instance
            ch.pipeline().addLast(new ClientMessageHandler());
        }
    }
}