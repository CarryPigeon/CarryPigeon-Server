package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/** 消息变更记录；eventId 只用于去重，sequence 才是频道内按提交顺序恢复的位置，payload 为协议 JSON。 */
public record MessageEventRecord(long eventId, long channelId, long sequence, String type, String payload, Instant createdAt) { }
