package answers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

class Server {

    private InetSocketAddress socketAddress = new InetSocketAddress(8080);

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

    void start() throws Exception {
        var eventLoopGroup = new NioEventLoopGroup();
        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup) // sets the EventLoopGroup that provides EventLoops for processing Channel events
                    .channel(NioServerSocketChannel.class) // Channel implementation to be used
                    .localAddress(socketAddress)
                    // Sets a ChannelInboundHandler for I/O and data for the accepted channels
                    .childHandler(new ChildChannelHandler());

            var binding = bootstrap.bind().sync();
            binding.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }

    private static class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel channel) { // initialize each new Channel with an server.EchoServerHandler instance
            channel.pipeline().addLast(new ServerMessageHandler());
        }
    }
}