package team.carrypigeon.backend.chat.domain.features.plugin.domain.command;

import java.time.Instant;
import java.util.Map;

/**
 * 消息 domain data 校验命令。
 * 边界：只携带插件校验所需的 canonical 消息上下文，不依赖 message feature 模型。
 */
public record ValidateMessageDataCommand(
        long messageId,
        long channelId,
        long senderId,
        Instant sendTime,
        String domain,
        String domainVersion,
        Map<String, Object> data,
        boolean clientRequest
) {
}
