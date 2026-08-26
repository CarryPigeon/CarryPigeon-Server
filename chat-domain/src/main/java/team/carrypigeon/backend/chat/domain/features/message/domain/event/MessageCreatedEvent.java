package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 频道消息已创建事件。
 * 职责：以 canonical 消息字段表达持久化成功的消息事实及接收账号快照。
 * 边界：不携带 message 模型、server 命令或 WebSocket frame。
 */
public record MessageCreatedEvent(
        long messageId,
        long senderId,
        long channelId,
        String domain,
        String domainVersion,
        Map<String, Object> data,
        Instant sendTime,
        List<Long> mentions,
        String preview,
        String status,
        List<Long> recipientAccountIds
) {

    public MessageCreatedEvent {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        mentions = List.copyOf(mentions);
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}
