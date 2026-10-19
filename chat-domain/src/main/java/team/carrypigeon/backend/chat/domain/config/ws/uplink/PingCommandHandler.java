package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import java.util.Map;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandContext;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 心跳命令处理器。
 * 职责：处理 {@code ping}：回复 {@code pong} 并回显请求 ID，供客户端做保活配对。
 * 边界：不刷新认证状态、不触发续传。
 */
@Component
public class PingCommandHandler implements RealtimeCommandHandler {

    /** 命令类型。 */
    public static final String COMMAND_TYPE = "ping";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "pong";

    private final TimeProvider timeProvider;

    public PingCommandHandler(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
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
    public void handle(RealtimeCommandContext context, RealtimeClientMessage command) {
        context.reply(new RealtimeFrame.Ack(
                ACK_TYPE,
                command.id(),
                Map.of("server_time", timeProvider.nowMillis())
        ));
    }
}
