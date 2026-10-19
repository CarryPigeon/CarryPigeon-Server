package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道成员数据库记录契约。
 * 职责：表达 chat_channel_member 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载成员管理业务规则。
 *
 * @param memberId 成员关系 ID
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param role 成员角色
 * @param joinedAt 加入时间
 */
public record ChannelMemberRecord(
        long memberId,
        long channelId,
        long accountId,
        String role,
        Instant joinedAt
) {
}
