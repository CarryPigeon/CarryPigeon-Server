package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道入群申请领域模型。
 * 职责：表达账号主动申请加入频道后产生的申请记录及其审批状态。
 * 边界：申请记录不等同于活跃成员，审批通过前不会进入 ChannelMember 投影；已处理的申请记录会被同一账号的新申请替换，因此不充当完整历史台账。
 *
 * @param applicationId 申请记录主键
 * @param applicantId 提交申请的账户 ID
 * @param channelId 申请加入的频道 ID
 * @param reason 申请理由；允许空字符串，不允许为 {@code null}
 * @param status 申请当前状态
 * @param createdAt 申请创建时间
 * @param respondedAt 审批响应时间；尚未审批时为空
 */
public record ChannelApplication(
        long applicationId,
        long applicantId,
        long channelId,
        String reason,
        ChannelApplicationStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
