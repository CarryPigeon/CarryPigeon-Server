package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道已读状态数据库记录契约。
 * 职责：表达 chat_channel_read_state 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载未读计算规则。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param lastReadMessageId 最后已读消息 ID；0 表示尚未产生已读锚点
 * @param lastReadTime 最后已读时间；未读时为 null
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelReadStateRecord(
        long channelId,
        long accountId,
        long lastReadMessageId,
        Instant lastReadTime,
        Instant createdAt,
        Instant updatedAt
) {
}
