package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandContext;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;

/**
 * 凭证替换命令处理器。
 * 职责：处理 {@code reauth}：在已认证连接上替换 access token，回复 {@code reauth.ok}，并按声明锚点触发续传。
 * 边界：认证状态机由连接侧承担；本命令要求连接已认证。
 */
@Component
public class ReauthCommandHandler implements RealtimeCommandHandler {

    /** 命令类型。 */
    public static final String COMMAND_TYPE = "reauth";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "reauth.ok";

    @Override
    public String commandType() {
        return COMMAND_TYPE;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public void handle(RealtimeCommandContext commandContext, RealtimeClientMessage command) {
        RealtimeCommandScope scope = (RealtimeCommandScope) commandContext;
        RealtimeAuthSession session = scope.authenticate(command.accessToken(), command.id(), true);
        scope.reply(new RealtimeFrame.Ack(ACK_TYPE, command.id(), session.ackData()));
        scope.replay(session.accountId(), scope.lastEventId());
    }
}
