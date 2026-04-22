package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道成员数据库记录契约。
 * 职责：为 chat-domain 与 database-impl 之间传递频道成员字段。
 * 边界：这里只表达数据库服务契约，不承载成员业务规则。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param joinedAt 加入时间
 */
public record ChannelMemberRecord(long channelId, long accountId, Instant joinedAt) {
}
