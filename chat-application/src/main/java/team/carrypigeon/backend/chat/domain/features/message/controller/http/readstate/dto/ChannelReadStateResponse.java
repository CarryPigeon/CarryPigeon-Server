package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道已读状态响应。
 */
public record ChannelReadStateResponse(
        @Schema(type = "string", description = "频道 ID", example = "12345") @JsonSerialize(using = ToStringSerializer.class) long cid,
        @Schema(type = "string", description = "账户 ID", example = "67890") @JsonSerialize(using = ToStringSerializer.class) long uid,
        @Schema(type = "string", description = "最后已读消息 ID", example = "100") @JsonSerialize(using = ToStringSerializer.class) long lastReadMid
) {
}
