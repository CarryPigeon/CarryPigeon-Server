package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 入群申请响应。
 */
public record ChannelApplicationResponse(
        @Schema(description = "申请 ID", example = "1") String applicationId,
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "申请用户 ID", example = "67890") String uid,
        @Schema(description = "申请理由", example = "hi") String reason,
        @Schema(description = "申请时间（epoch 毫秒）", example = "1700000000000") long applyTime,
        @Schema(description = "申请状态", example = "pending") String status
) {
}
