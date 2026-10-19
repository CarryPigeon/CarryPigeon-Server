package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandContext;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;

/**
 * 首帧认证命令处理器。
 * 职责：处理 {@code auth}：校验 access token、绑定连接主体、回复 {@code auth.ok}，并按声明锚点触发续传。
 * 边界：认证状态机由连接侧承担；已认证连接不允许再次 auth，必须改用 {@code reauth}。
 */
@Component
public class AuthCommandHandler implements RealtimeCommandHandler {

    /** 命令类型。 */
    public static final String COMMAND_TYPE = "auth";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "auth.ok";

    @Override
    public String commandType() {
        return COMMAND_TYPE;
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Override
    public void handle(RealtimeCommandContext commandContext, RealtimeClientMessage command) {
        RealtimeCommandScope scope = (RealtimeCommandScope) commandContext;
        if (scope.isAuthenticated()) {
            scope.reply(new RealtimeFrame.CommandError(
                    command.id(),
                    COMMAND_TYPE,
                    "already_authenticated",
                    "use reauth to replace credentials"
            ));
            return;
        }
        RealtimeAuthSession session = scope.authenticate(command.accessToken(), command.id(), false);
        scope.reply(new RealtimeFrame.Ack(ACK_TYPE, command.id(), session.ackData()));
        scope.replay(session.accountId(), scope.lastEventId());
    }
}
