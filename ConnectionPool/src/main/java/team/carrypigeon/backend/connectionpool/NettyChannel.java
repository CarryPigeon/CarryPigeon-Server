package team.carrypigeon.backend.connectionpool;

import io.netty.channel.ChannelHandlerContext;
import team.carrypigeon.backend.api.domain.CPChannel;
import team.carrypigeon.backend.api.domain.bo.user.CPUserBO;

public class NettyChannel implements CPChannel {

    private CPUserBO cpUserBO;
    private final ChannelHandlerContext context;

    public NettyChannel(ChannelHandlerContext context) {
        this.context = context;
    }

    @Override
    public void sendMessage(String msg) {
        context.writeAndFlush(msg);
    }

    @Override
    public CPUserBO getCPUserBO() {
        return cpUserBO;
    }

    @Override
    public void setCPUserBO(CPUserBO cpUserBO) {
        this.cpUserBO = cpUserBO;
    }
}
