package team.carrypigeon.backend.chat.domain.features.server.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.SendFileMessageRealtimeHandler;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageHandler;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.SendChannelMessageRealtimeHandler;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.SendVoiceMessageRealtimeHandler;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * realtime 入站消息处理装配配置。
 * 职责：在 server feature 内装配当前运行时可用的入站消息处理器与分发器。
 * 边界：这里只负责入站处理链装配，不承载具体消息业务逻辑。
 */
@Configuration
public class RealtimeMessageHandlingConfiguration {

    /**
     * 创建频道消息实时入站处理器。
     *
     * @return 频道消息实时入站处理器
     */
    @Bean
    public RealtimeInboundMessageHandler sendChannelMessageRealtimeHandler() {
        return new SendChannelMessageRealtimeHandler();
    }

    /**
     * 创建文件消息实时入站处理器。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 文件消息实时入站处理器
     */
    @Bean
    public RealtimeInboundMessageHandler sendFileMessageRealtimeHandler(JsonProvider jsonProvider) {
        return new SendFileMessageRealtimeHandler(jsonProvider);
    }

    /**
     * 创建语音消息实时入站处理器。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 语音消息实时入站处理器
     */
    @Bean
    public RealtimeInboundMessageHandler sendVoiceMessageRealtimeHandler(JsonProvider jsonProvider) {
        return new SendVoiceMessageRealtimeHandler(jsonProvider);
    }

    /**
     * 创建 realtime 入站消息分发器。
     *
     * @param handlers 当前运行时可用入站处理器列表
     * @return realtime 入站消息分发器
     */
    @Bean
    public RealtimeInboundMessageDispatcher realtimeInboundMessageDispatcher(List<RealtimeInboundMessageHandler> handlers) {
        return new RealtimeInboundMessageDispatcher(handlers);
    }
}
