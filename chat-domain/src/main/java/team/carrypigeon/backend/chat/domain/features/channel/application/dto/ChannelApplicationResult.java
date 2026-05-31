package team.carrypigeon.backend.chat.domain.features.channel.application.dto;

import java.time.Instant;

/**
 * 入群申请结果。
 */
public record ChannelApplicationResult(long applicationId, long channelId, long accountId, String reason, Instant applyTime, String status) {
}
