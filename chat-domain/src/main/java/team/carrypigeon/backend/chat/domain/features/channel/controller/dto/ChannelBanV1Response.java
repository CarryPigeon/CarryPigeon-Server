package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * v1 频道禁言响应。
 */
public record ChannelBanV1Response(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "被禁言用户 ID", example = "67890") String uid,
        @Schema(description = "禁言截止时间（epoch 毫秒）", example = "1700003600000") Long until,
        @Schema(description = "禁言原因", example = "spam") String reason,
        @Schema(description = "创建时间（epoch 毫秒）", example = "1700000000000") long createTime
) {
}
