package team.carrypigeon.backend.chat.domain.features.message.domain.projection;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;

/**
 * 频道消息 canonical 结果投影。
 * 职责：向协议层和跨 feature 调用方暴露统一消息字段。
 * 边界：不包含发送者资料快照、编辑字段或 domain 专属顶层字段。
 */
public record ChannelMessageResult(
        long messageId,
        long senderId,
        long channelId,
        String domain,
        String domainVersion,
        Map<String, Object> data,
        Instant sendTime,
        List<Long> mentions,
        String preview,
        MessageStatus status
) {
}
