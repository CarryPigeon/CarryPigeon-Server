package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;

/**
 * 入群申请结果。
 */
public record ChannelApplicationResult(long applicationId, long channelId, long accountId, String reason, Instant applyTime, String status) {

    public static ChannelApplicationResult from(ChannelInvite invite, String reason) {
        return new ChannelApplicationResult(
                invite.applicationId(),
                invite.channelId(),
                invite.inviteeAccountId(),
                reason == null ? invite.reason() : reason,
                invite.createdAt(),
                invite.status().name()
        );
    }
}
