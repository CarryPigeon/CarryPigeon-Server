package team.carrypigeon.backend.chat.domain.config.ws.uplink.commands;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthSession;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;

/**
 * 凭证替换命令处理器。
 * 职责：处理 {@code reauth}：在已认证连接上替换 access token，回复 {@code reauth.ok}，并按声明锚点触发续传。
 * 边界：认证状态机由连接侧承担；本命令要求连接已认证。
 */
@Component
public class ReAuthCommandHandler implements RealtimeCommandHandler {

    private final RealtimeFrameCodec frameCodec;


    /** 命令类型。 */
    public static final String COMMAND_TYPE = "reauth";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "reauth.ok";

    public ReAuthCommandHandler(RealtimeFrameCodec frameCodec) {
        this.frameCodec = frameCodec;
    }

    @Override
    public String commandType() {
        return COMMAND_TYPE;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public void handle(RealtimeClientMessage message, RealtimeConnection connection, RealtimeAttributes attributes, RealtimeScheduler scheduler, RealtimeAuthState realtimeAuthState) {
        String accessToken = message.accessToken();
        if (accessToken == null || accessToken.isBlank()){
            throw ProblemException.of(AuthProblem.AUTHENTICATION_REQUIRED);
        }
        RealtimeAuthSession authenticate = realtimeAuthState.authenticate(connection, attributes, scheduler, accessToken);
        connection.send(frameCodec.encode(new RealtimeFrame.Ack(ACK_TYPE, message.id(), authenticate.ackData())));
    }
}
