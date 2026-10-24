package team.carrypigeon.backend.infrastructure.basic.plugin.message;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 频道消息 canonical 领域模型。
 * 职责：以统一字段表达任意 domain 消息，并把所有 domain 专属内容收口到 data。
 * 边界：mentions 是通用提醒元数据；preview 是派生摘要；模型不承载编辑或旧载荷兼容字段。
 *
 * @param messageId 消息 ID
 * @param senderId 发送者账号 ID
 * @param channelId 所属频道 ID
 * @param domain 消息 domain
 * @param data domain 专属 canonical 数据
 * @param sendTime 服务端发送时间
 * @param mentions 需要提醒的用户 ID 列表
 * @param preview 服务端派生摘要
 * @param status 消息状态
 */
public record CPChannelMessage(
        long messageId,
        long senderId,
        long channelId,
        String domain,
        Map<String, Object> data,
        Instant sendTime,
        List<Long> mentions,
        String preview,
        CPMessageStatus status
) {

    public CPChannelMessage {
        data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
        preview = preview == null ? "" : preview;
    }
}
