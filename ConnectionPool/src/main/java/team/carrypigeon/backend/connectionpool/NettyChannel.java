package team.carrypigeon.backend.connectionpool;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.connectionpool.security.CPClientSecurity;
import team.carrypigeon.backend.connectionpool.security.aes.AESUtil;

@Slf4j
public class NettyChannel implements CPChannel {

    private CPUserBO cpUserBO;
    private final ChannelHandlerContext context;
    private final CPClientSecurity security;

    public NettyChannel(ChannelHandlerContext context, CPClientSecurity security) {
        this.context = context;
        this.security = security;
    }

    @Override
    public void sendMessage(String msg) {
        try {
            context.writeAndFlush(AESUtil.encrypt(msg,AESUtil.convertStringToKey(security.getKey())));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
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
