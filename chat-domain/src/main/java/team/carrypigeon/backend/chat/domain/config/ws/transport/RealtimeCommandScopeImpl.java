package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeResumeCoordinator;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeAuthSession;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeCommandScope;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;

/**
 * 实时命令作用域实现。
 * 职责：把一次命令处理绑定到当前连接、当前命令与回复出口，并实现内建命令所需的认证与续传能力。
 * 边界：只做转发与绑定；认证判断、续传策略仍分别由认证状态机与续传协调器承担。
 */
final class RealtimeCommandScopeImpl implements RealtimeCommandScope {

    private final ChannelHandlerContext context;
    private final RealtimeClientMessage command;
    private final RealtimeAuthenticationCoordinator authenticationCoordinator;
    private final RealtimeResumeCoordinator resumeCoordinator;
    private final RealtimeFrameCodec frameCodec;

    RealtimeCommandScopeImpl(
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
    public boolean isAuthenticated() {
        return authenticationCoordinator.isAuthenticated(context);
    }

    @Override
    public RealtimeAuthSession authenticate(String accessToken, String requestId, boolean reauth) {
        return authenticationCoordinator.authenticate(context, accessToken, requestId, reauth);
    }

    @Override
    public void replay(long accountId, String lastEventId) {
        resumeCoordinator.replay(context, accountId, lastEventId);
    }
}
