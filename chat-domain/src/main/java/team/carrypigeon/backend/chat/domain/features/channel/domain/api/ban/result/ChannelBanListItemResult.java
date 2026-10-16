package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道禁言列表项结果。
 * 职责：向协议层暴露单条禁言记录的稳定字段。
 * 边界：只承载应用层返回数据，不承载禁言状态流转逻辑。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被禁言账户 ID
 * @param expiresAt 禁言到期时间；为空表示不自动到期
 * @param reason 禁言原因
 * @param createdAt 禁言创建时间
 */
public record ChannelBanListItemResult(long channelId, long bannedAccountId, Instant expiresAt, String reason, Instant createdAt) {

    public static ChannelBanListItemResult from(ChannelBan ban) {
        return new ChannelBanListItemResult(
                ban.channelId(),
                ban.bannedAccountId(),
                ban.expiresAt(),
                ban.reason(),
                ban.createdAt()
        );
    }
}
