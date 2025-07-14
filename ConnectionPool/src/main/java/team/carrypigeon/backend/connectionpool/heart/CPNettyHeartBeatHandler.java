package team.carrypigeon.backend.connectionpool.heart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class CPNettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private ObjectMapper mapper;

    public CPNettyHeartBeatHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    // 读空闲则关闭连接
                    ctx.channel().close();
                    break;
                case WRITER_IDLE:
                    // 写空闲则发送心跳包
                    ctx.channel().writeAndFlush(mapper.writeValueAsString(HeartBeatMessage.INSTANCE));
                    break;
                case ALL_IDLE:
                    ctx.channel().close();
                    break;
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}