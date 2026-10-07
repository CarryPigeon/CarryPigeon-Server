package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道封禁列表项结果。
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
