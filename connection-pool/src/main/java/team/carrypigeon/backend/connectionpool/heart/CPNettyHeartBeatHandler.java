package team.carrypigeon.backend.connectionpool.heart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPChannel;

import static team.carrypigeon.backend.connectionpool.ConnectionConstant.CHANNEL;

@Slf4j
public class CPNettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private final ObjectMapper mapper;

    public CPNettyHeartBeatHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    log.debug("读空闲");
                    // 读空闲则关闭连接
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    log.debug("写空闲");
                    // 写空闲则发送心跳包
                    CPChannel cpChannel = ctx.channel().attr(CHANNEL).get();
                    if (cpChannel != null){
                        cpChannel.sendMessage(mapper.writeValueAsString(HeartBeatMessage.HEARTBEAT));
                    }else {
                        ctx.writeAndFlush(mapper.writeValueAsString(HeartBeatMessage.HEARTBEAT));
                    }
                    break;
                case ALL_IDLE:
                    log.debug("读写空闲");
                    ctx.close();
                    break;
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}