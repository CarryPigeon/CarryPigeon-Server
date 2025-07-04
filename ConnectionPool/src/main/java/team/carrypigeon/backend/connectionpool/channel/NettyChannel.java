package team.carrypigeon.backend.connectionpool.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.connection.pool.CPChannel;
import team.carrypigeon.backend.connectionpool.ConnectionHandler;

public class NettyChannel implements CPChannel {

    private final ChannelHandlerContext  context;

    private static final AttributeKey<ConnectionHandler> CHANNEL_DATA = AttributeKey.valueOf("Channel");

    public NettyChannel(ChannelHandlerContext context) {
        this.context = context;
    }

    @Override
    public void write(String msg) {
        context.channel().writeAndFlush(msg);
    }

    @Override
    public void getData() {
        context.channel().attr(CHANNEL_DATA).get();
    }
}
