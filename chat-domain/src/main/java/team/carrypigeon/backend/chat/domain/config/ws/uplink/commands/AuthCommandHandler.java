package team.carrypigeon.backend.chat.domain.config.ws.uplink.commands;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthSession;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;

/**
 * 首帧认证命令处理器。
 * 职责：处理 {@code auth}：校验 access token、绑定连接主体、回复 {@code auth.ok}，并按声明锚点触发续传。
 * 边界：认证状态机由连接侧承担；已认证连接不允许再次 auth，必须改用 {@code reauth}。
 */
@Component
public class AuthCommandHandler implements RealtimeCommandHandler {

    private final RealtimeFrameCodec frameCodec;

    /** 命令类型。 */
    public static final String COMMAND_TYPE = "auth";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "auth.ok";

    public AuthCommandHandler(RealtimeFrameCodec frameCodec) {
        this.frameCodec = frameCodec;
    }

    @Override
    public String commandType() {
        return COMMAND_TYPE;
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Override
    public void handle(RealtimeClientMessage message, RealtimeConnection connection, RealtimeAttributes attributes, RealtimeScheduler scheduler, RealtimeAuthState realtimeAuthState) {
        // 校验 access token
        String accessToken = message.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw ProblemException.of(AuthProblem.AUTHENTICATION_REQUIRED);
        }
        // 进行认证
        RealtimeAuthSession authenticate = realtimeAuthState.authenticate(connection, attributes, scheduler, accessToken);
        // 进行应答
        connection.send(frameCodec.encode(new RealtimeFrame.Ack(ACK_TYPE,message.id(),authenticate.ackData())));
    }
}
