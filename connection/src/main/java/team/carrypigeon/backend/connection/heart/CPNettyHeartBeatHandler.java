package team.carrypigeon.backend.connection.heart;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.connection.disconnect.DisconnectSupport;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

public class CPNettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    // 读空闲则关闭连接
                    DisconnectSupport.markDisconnect(ctx.channel(), ctx.channel().attr(SESSIONS).get(),
                            "idle_timeout_reader", "IdleState", "READER_IDLE");
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    // 写空闲则发送心跳包
                    CPSession session = ctx.channel().attr(SESSIONS).get();
                    if (session == null){
                        return;
                    }
                    // 心跳包不需要加密
                    session.write("",false);
                    break;
                case ALL_IDLE:
                    DisconnectSupport.markDisconnect(ctx.channel(), ctx.channel().attr(SESSIONS).get(),
                            "idle_timeout_all", "IdleState", "ALL_IDLE");
                    ctx.close();
                    break;
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
