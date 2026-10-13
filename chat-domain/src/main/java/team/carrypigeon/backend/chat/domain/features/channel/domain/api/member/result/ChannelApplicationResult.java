package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result;



import java.time.Instant;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;

/**
 * 频道邀请结果。
 * 职责：向协议层暴露邀请记录在当前切片内的稳定字段。
 * 边界：只承载应用层返回数据，不承载邀请状态流转逻辑。
 *
 * @param applicationId 邀请 ID
 * @param channelId 频道 ID
 * @param applicantId 被邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间
 */
public record ChannelApplicationResult(
        long applicationId,
        long channelId,
        long applicantId,
        String status,
        Instant createdAt,
        Instant respondedAt
) {

    public static ChannelApplicationResult from(ChannelApplication application) {
        return new ChannelApplicationResult(
            application.applicationId(),
            application.channelId(),
            application.applicantId(),
            application.status().name(),
            application.createdAt(),
            application.respondedAt()
        );
    }
}
