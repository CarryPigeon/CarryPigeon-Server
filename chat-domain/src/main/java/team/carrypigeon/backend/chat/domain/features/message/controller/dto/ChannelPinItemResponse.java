package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道置顶列表项响应。
 */
public record ChannelPinItemResponse(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "消息 ID", example = "723155640365318144") String mid,
        @Schema(description = "置顶者用户 ID", example = "67890") String pinnedByUid,
        @Schema(description = "置顶时间", example = "1700000000000") long pinnedAt,
        @Schema(description = "备注", example = "重要通知") String note
) {
}
