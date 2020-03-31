package workshop;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import static io.netty.channel.ChannelHandler.Sharable;

@Sharable
class ClientMessageHandlerWorkshop extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // write and flush Hello Netty!, hint: writeAndFlush, Unpooled.copiedBuffer, CharsetUtil.UTF_8
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        // print to console: Client received: incoming message, hint:System.out.println, in.toString, CharsetUtil.UTF_8
        // close connection, hint: ctx.close()
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // print stacktrace, hint: cause.printStackTrace()
        // close connection, hint: ctx.close()
    }
}
