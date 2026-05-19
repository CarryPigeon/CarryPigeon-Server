package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道成员数据库记录契约。
 * 职责：表达频道成员读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载成员管理业务规则。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param role 固定角色
 * @param joinedAt 加入时间
 * @param mutedUntil 禁言截止时间；为空表示当前未禁言
 */
public record ChannelMemberRecord(
        long channelId,
        long accountId,
        String role,
        Instant joinedAt,
        Instant mutedUntil
) {
}
