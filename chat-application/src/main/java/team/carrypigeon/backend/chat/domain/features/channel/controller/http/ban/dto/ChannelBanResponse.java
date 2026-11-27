package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道禁言响应。
 * 职责：承载禁言或解除禁言动作后的协议字段。
 * 边界：只暴露单条禁言结果，不暴露申请与成员治理语义。
 */
public record ChannelBanResponse(
        @Schema(type = "string", description = "频道 ID", example = "12345") @JsonSerialize(using = ToStringSerializer.class) long cid,
        @Schema(type = "string", description = "被禁言账户 ID", example = "67890") @JsonSerialize(using = ToStringSerializer.class) long uid,
        @Schema(description = "禁言截止时间（epoch 毫秒）；新记录必有值，历史数据可能为空（按未生效处理）", example = "1700003600000") Long until,
        @Schema(description = "禁言原因", example = "spam") String reason,
        @Schema(description = "禁言创建时间（epoch 毫秒）", example = "1700000000000") long createTime
) {
}
