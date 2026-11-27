package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 未读频道项响应。
 */
public record UnreadItemResponse(
        @Schema(description = "频道 ID", example = "1") String cid,
        @Schema(description = "频道有效未读消息数，10000 表示至少一万条", example = "10000") long unreadCount
) {
}
