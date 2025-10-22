package team.carrypigeon.backend.connection.heart;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

@Slf4j
public class CPNettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

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
                    CPSession session = ctx.channel().attr(SESSIONS).get();
                    if (session == null){
                        return;
                    }
                    // 心跳包不需要加密
                    session.write("",false);
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