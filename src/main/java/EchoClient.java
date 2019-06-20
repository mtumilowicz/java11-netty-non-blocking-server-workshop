import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * Created by mtumilowicz on 2019-04-05.
 */
class EchoClient {
    private final String host = "localhost";
    private final int port = 8080;

    public static void main(String[] args) throws Exception {
        new EchoClient().start();
    }

    void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // Creates a Bootstrap to create and connect new client channels
            /*
            Sets the EventLoopGroup
            that provides EventLoops for
            processing Channel events
             */
            b.group(group)
                    /*
                    Specifies the Channel
                    implementation to
                    be used
                     */
                    .channel(NioSocketChannel.class)
                    /*
                    instead you could directly Connects to the remote host
                    
                    */
                    .remoteAddress(new InetSocketAddress(host, port))
                    /*
                    Sets the handler
                    for Channel events
                    and data
                     */
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new EchoClientHandler());
                        }
                    });
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}