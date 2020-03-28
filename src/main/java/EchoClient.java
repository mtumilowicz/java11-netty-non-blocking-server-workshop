import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

class EchoClient {

    private final InetSocketAddress socketAddress = new InetSocketAddress("localhost", 8080);

    public static void main(String[] args) throws Exception {
        new EchoClient().start();
    }

    void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // Creates a Bootstrap to create and connect new client channels
            b.group(group) // set EventLoopGroup that provides EventLoops for processing Channel events
                    .channel(NioSocketChannel.class) // channel implementation
                    .remoteAddress(socketAddress)
                    .handler(new ChannelInitializer<SocketChannel>() { // handler for Channel events and data
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}