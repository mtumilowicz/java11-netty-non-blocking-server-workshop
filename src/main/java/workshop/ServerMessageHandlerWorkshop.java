package workshop;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import static io.netty.channel.ChannelHandler.Sharable;

@Sharable
class ServerMessageHandlerWorkshop extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // cast message to ByteBuf
        // print to console: Server received: incoming message, hint:System.out.println, toString, CharsetUtil.UTF_8
        // echo that message, hint: writeAndFlush
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // print stacktrace, hint: cause.printStackTrace()
        // close connection, hint: ctx.close()
    }
}