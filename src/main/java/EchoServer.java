import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

class EchoServer {

    private InetSocketAddress socketAddress = new InetSocketAddress(8080);

    public static void main(String[] args) throws Exception {
        new EchoServer().start();
    }

    void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group) // sets the EventLoopGroup that provides EventLoops for processing Channel events
                    .channel(NioServerSocketChannel.class) // Channel implementation to be used
                    .localAddress(socketAddress)
                    // Sets a ChannelInboundHandler for I/O and data for the accepted channels
                    .childHandler(new ChildChannelHandler());
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    private static class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) { // initialize each new Channel with an EchoServerHandler instance
            ch.pipeline().addLast(new EchoServerHandler());
        }
    }
}