package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandContext;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;

/**
 * 认证类命令处理器。
 * 职责：承担 {@code auth} 与 {@code reauth} 两个命令，二者只在命令名、应答名与"是否要求已认证"上不同。
 * 边界：认证机制由 {@link RealtimeAuthenticationCoordinator} 承担，续传由上下文回调触发。
 */
public final class AuthCommandHandler implements RealtimeCommandHandler {

    private final String commandType;
    private final String ackType;
    private final boolean requiresAuthentication;

    /**
     * 创建认证类命令处理器。
     *
     * @param commandType 命令类型
     * @param ackType 成功应答类型
     * @param requiresAuthentication 是否要求连接已认证；为真时即为凭据替换语义
     */
    public AuthCommandHandler(String commandType, String ackType, boolean requiresAuthentication) {
        this.commandType = commandType;
        this.ackType = ackType;
        this.requiresAuthentication = requiresAuthentication;
    }

    @Override
    public String commandType() {
        return commandType;
    }

    @Override
    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    @Override
    public void handle(RealtimeCommandContext commandContext, RealtimeClientMessage command) {
        RealtimeTransportCommandContext context = (RealtimeTransportCommandContext) commandContext;
        RealtimeAuthenticationCoordinator authentication = context.authenticationCoordinator();
        if (!requiresAuthentication && authentication.isAuthenticated(context.nettyContext())) {
            context.reply(new RealtimeFrame.CommandError(
                    command.id(),
                    commandType,
                    "already_authenticated",
                    "use reauth to replace credentials"
            ));
            return;
        }
        RealtimeAuthenticationCoordinator.AuthenticatedSession session = authentication.authenticate(
                context.nettyContext(),
                command,
                requiresAuthentication
        );
        context.reply(new RealtimeFrame.Ack(ackType, command.id(), authentication.ackData(session)));
        context.replay(session.accountId(), context.lastEventId());
    }
}
