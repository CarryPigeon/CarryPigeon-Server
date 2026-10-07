package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道封禁结果。
 * 职责：向协议层暴露频道封禁/解封动作后的稳定字段。
 * 边界：只承载应用层返回数据，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被封禁账户 ID
 * @param operatorAccountId 操作人账户 ID
 * @param reason 封禁原因
 * @param expiresAt 到期时间
 * @param createdAt 创建时间
 * @param revokedAt 解封时间
 */
public record ChannelBanResult(
        long channelId,
        long bannedAccountId,
        long operatorAccountId,
        String reason,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt
) {

    public static ChannelBanResult from(ChannelBan ban) {
        return new ChannelBanResult(
                ban.channelId(),
                ban.bannedAccountId(),
                ban.operatorAccountId(),
                ban.reason(),
                ban.expiresAt(),
                ban.createdAt(),
                ban.revokedAt()
        );
    }
}
