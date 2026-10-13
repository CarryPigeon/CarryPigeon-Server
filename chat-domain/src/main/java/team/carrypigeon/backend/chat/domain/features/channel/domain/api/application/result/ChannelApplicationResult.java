package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;

/**
 * 入群申请结果。
 */
public record ChannelApplicationResult(
    long applicationId,
    long channelId,
    long accountId,
    String reason,
    Instant applyTime,
    String status
) {

    public static ChannelApplicationResult from(ChannelApplication application, String reason) {
        return new ChannelApplicationResult(
                application.applicationId(),
                application.channelId(),
                application.applicantId(),
                reason == null ? application.reason() : reason,
                application.createdAt(),
                application.status().name()
        );
    }
}
