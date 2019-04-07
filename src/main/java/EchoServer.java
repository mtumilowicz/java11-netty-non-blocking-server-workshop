import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Created by mtumilowicz on 2019-04-05.
 */
class EchoServer {
    private final int port = 8080;

    public static void main(String[] args) throws Exception {
        new EchoServer().start();
    }

    void start() throws Exception {
        final EchoServerHandler serverHandler = new EchoServerHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            /*
            Creates a
            ServerBootstrap
             */
            ServerBootstrap b = new ServerBootstrap();
            /*
            Sets the EventLoopGroup
            that provides EventLoops for
            processing Channel events
             */
            b.group(group)
                    /*
                    Specifies
the Channel
implementation
to be used
                     */
                    .channel(NioServerSocketChannel.class)
                    /*
                    Binds the
channel with
the configured
bootstrap

instead
bootstrap.bind(new InetSocketAddress(8080))
                     */
                    .localAddress(new InetSocketAddress(port))
                    /*
                    Sets a ChannelInboundHandler
for I/O and data for the
accepted channels

use new SimpleChannelInboundHandler<ByteBuf>
                     */
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(serverHandler);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
