package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道禁言列表项响应。
 * 职责：承载单条禁言记录的协议字段。
 * 边界：只暴露列表渲染所需字段，不暴露禁言操作者与解除时间。
 */
public record ChannelBanListItemResponse(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "被禁言账户 ID", example = "67890") String uid,
        @Schema(description = "禁言截止时间（epoch 毫秒）；为空表示不自动到期", example = "1700003600000") Long until,
        @Schema(description = "禁言原因", example = "spam") String reason,
        @Schema(description = "禁言创建时间（epoch 毫秒）", example = "1700000000000") long createTime
) {
}
