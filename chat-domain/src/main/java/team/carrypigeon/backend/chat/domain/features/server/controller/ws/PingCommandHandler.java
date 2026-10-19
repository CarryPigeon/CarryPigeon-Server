package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandContext;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 心跳命令处理器。
 * 职责：回复 pong 并回显请求 ID，供客户端做保活配对。
 * 边界：不刷新认证状态、不触发续传。
 */
public final class PingCommandHandler implements RealtimeCommandHandler {

    private final TimeProvider timeProvider;

    public PingCommandHandler(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public String commandType() {
        return "ping";
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public void handle(RealtimeCommandContext context, RealtimeClientMessage command) {
        context.reply(new RealtimeFrame.Ack(
                "pong",
                command.id(),
                Map.of("server_time", timeProvider.nowMillis())
        ));
    }
}
