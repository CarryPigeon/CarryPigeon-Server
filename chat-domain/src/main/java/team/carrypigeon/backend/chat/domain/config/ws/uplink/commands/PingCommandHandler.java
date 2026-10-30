package team.carrypigeon.backend.chat.domain.config.ws.uplink.commands;

import java.util.Map;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 心跳命令处理器。
 * 职责：处理 {@code ping}：回复 {@code pong} 并回显请求 ID，供客户端做保活配对。
 * 边界：不刷新认证状态、不触发续传。
 */
@Component
public class PingCommandHandler implements RealtimeCommandHandler {

    private final RealtimeFrameCodec frameCodec;
    private final TimeProvider timeProvider;

    /** 命令类型。 */
    public static final String COMMAND_TYPE = "ping";

    /** 成功应答类型。 */
    public static final String ACK_TYPE = "pong";


    public PingCommandHandler(RealtimeFrameCodec frameCodec, TimeProvider timeProvider) {
        this.frameCodec = frameCodec;
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
    public void handle(RealtimeClientMessage message, RealtimeConnection connection, RealtimeAttributes attributes, RealtimeScheduler scheduler, RealtimeAuthState realtimeAuthState) {
        connection.send(
            frameCodec.encode(new RealtimeFrame.Ack(
                ACK_TYPE,
                message.id(),
                Map.of("server_time", timeProvider.nowMillis())
            ))
        );
    }
}
