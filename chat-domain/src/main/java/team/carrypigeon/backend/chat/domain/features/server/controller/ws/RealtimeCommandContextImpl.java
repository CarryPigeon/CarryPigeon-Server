package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeFrameCodec;

/**
 * 传输层命令上下文实现。
 * 职责：把当前连接、当前命令与回复出口绑定为一次命令处理上下文。
 * 边界：只做转发，不承载认证判断或续传逻辑。
 */
final class RealtimeCommandContextImpl implements RealtimeTransportCommandContext {

    private final ChannelHandlerContext context;
    private final RealtimeClientMessage command;
    private final RealtimeAuthenticationCoordinator authenticationCoordinator;
    private final RealtimeResumeCoordinator resumeCoordinator;
    private final RealtimeFrameCodec frameCodec;

    RealtimeCommandContextImpl(
            ChannelHandlerContext context,
            RealtimeClientMessage command,
            RealtimeAuthenticationCoordinator authenticationCoordinator,
            RealtimeResumeCoordinator resumeCoordinator,
            RealtimeFrameCodec frameCodec
    ) {
        this.context = context;
        this.command = command;
        this.authenticationCoordinator = authenticationCoordinator;
        this.resumeCoordinator = resumeCoordinator;
        this.frameCodec = frameCodec;
    }

    @Override
    public long accountId() {
        if (!authenticationCoordinator.isAuthenticated(context)) {
            // 契约约定 accountId 只在需要认证的命令里可用，未认证时提前失败而不是返回 0
            throw new IllegalStateException("accountId is only available on authenticated connections");
        }
        return authenticationCoordinator.accountId(context);
    }

    @Override
    public void reply(RealtimeFrame frame) {
        context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(frame)));
    }

    @Override
    public String lastEventId() {
        return command.lastEventId();
    }

    @Override
    public ChannelHandlerContext nettyContext() {
        return context;
    }

    @Override
    public RealtimeAuthenticationCoordinator authenticationCoordinator() {
        return authenticationCoordinator;
    }

    @Override
    public void replay(long accountId, String lastEventId) {
        resumeCoordinator.replay(context, accountId, lastEventId);
    }
}
