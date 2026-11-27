package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 未读频道项响应。
 */
public record UnreadItemResponse(
        @Schema(type = "string", description = "频道 ID", example = "1") @JsonSerialize(using = ToStringSerializer.class) long cid,
        @Schema(description = "频道有效未读消息数，10000 表示至少一万条", example = "10000") long unreadCount
) {
}
