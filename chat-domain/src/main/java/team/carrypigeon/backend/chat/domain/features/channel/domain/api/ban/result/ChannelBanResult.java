package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道禁言结果。
 * 职责：向协议层暴露禁言与解除禁言动作后的稳定字段。
 * 边界：只承载应用层返回数据，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被禁言账户 ID
 * @param operatorAccountId 操作人账户 ID
 * @param reason 禁言原因
 * @param expiresAt 禁言到期时间；为空表示不自动到期
 * @param createdAt 禁言创建时间
 * @param revokedAt 解除禁言时间；为空表示尚未解除
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
