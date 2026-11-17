package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;
import java.util.Map;

/** 可恢复的消息变更；数值 ID 保持 long，payload 是事件协议快照，sequence 仅在频道内有序。 */
public record MessageChange(long eventId, long sequence, String type, Map<String, Object> payload, Instant createdAt) { }
