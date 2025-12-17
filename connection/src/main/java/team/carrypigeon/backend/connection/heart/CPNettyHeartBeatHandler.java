package team.carrypigeon.backend.connection.heart;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

public class CPNettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CPNettyHeartBeatHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    log.info("read idle, closing connection, remoteAddress={}", ctx.channel().remoteAddress());
                    // 读空闲则关闭连接
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    log.debug("write idle, sending heartbeat, remoteAddress={}", ctx.channel().remoteAddress());
                    // 写空闲则发送心跳包
                    CPSession session = ctx.channel().attr(SESSIONS).get();
                    if (session == null){
                        return;
                    }
                    // 心跳包不需要加密
                    session.write("",false);
                    break;
                case ALL_IDLE:
                    log.info("all idle, closing connection, remoteAddress={}", ctx.channel().remoteAddress());
                    ctx.close();
                    break;
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
