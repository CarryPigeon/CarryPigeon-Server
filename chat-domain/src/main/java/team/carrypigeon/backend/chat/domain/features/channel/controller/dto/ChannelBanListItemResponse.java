package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道封禁列表项响应。
 */
public record ChannelBanListItemResponse(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "被禁言用户 ID", example = "67890") String uid,
        @Schema(description = "禁言截止时间", example = "1700003600000") Long until,
        @Schema(description = "原因", example = "spam") String reason,
        @Schema(description = "创建时间", example = "1700000000000") long createTime
) {
}
