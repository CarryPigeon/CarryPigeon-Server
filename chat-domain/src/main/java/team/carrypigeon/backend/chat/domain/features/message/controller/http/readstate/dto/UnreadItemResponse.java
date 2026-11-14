package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 未读频道项响应。
 */
public record UnreadItemResponse(
        @Schema(description = "频道 ID", example = "1") String cid,
        @Schema(description = "未读数量", example = "3") long unreadCount
) {
}
