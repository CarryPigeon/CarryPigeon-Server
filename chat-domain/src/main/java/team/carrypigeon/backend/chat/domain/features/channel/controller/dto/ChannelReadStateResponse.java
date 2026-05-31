package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道已读状态响应。
 */
public record ChannelReadStateResponse(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "账户 ID", example = "67890") String uid,
        @Schema(description = "最后已读消息 ID", example = "100") String lastReadMid,
        @Schema(description = "最后已读时间（epoch 毫秒）", example = "1700000000000") long lastReadTime
) {
}
