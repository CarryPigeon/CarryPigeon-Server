package team.carrypigeon.backend.connectionpool;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.connectionpool.channel.NettyChannel;

@AllArgsConstructor
public class ConnectionHandler extends SimpleChannelInboundHandler<String> {

    private CPControllerDispatcher cpControllerDispatcher;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        cpControllerDispatcher.process(new NettyChannel(ctx),msg);
    }
}