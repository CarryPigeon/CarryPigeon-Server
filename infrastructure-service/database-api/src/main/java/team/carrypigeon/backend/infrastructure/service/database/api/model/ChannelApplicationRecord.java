package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道申请数据库记录契约。
 * 职责：表达 chat_channel_application 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载申请审批业务规则。
 *
 * @param applicationId 申请 ID
 * @param applicantId 申请人账户 ID
 * @param channelId 频道 ID
 * @param reason 申请理由
 * @param status 申请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间；未响应时为空
 */
public record ChannelApplicationRecord(
        long applicationId,
        long applicantId,
        long channelId,
        String reason,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}
