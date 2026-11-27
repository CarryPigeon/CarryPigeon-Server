package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 入群申请响应。
 * 职责：承载单条入群申请的标识、申请人、理由、时间与状态字段。
 * 边界：只暴露申请展示字段，不暴露审批人与审批意见；状态以小写形式给出。
 */
public record ChannelApplicationResponse(
        @Schema(type = "string", description = "申请 ID", example = "1") @JsonSerialize(using = ToStringSerializer.class) long applicationId,
        @Schema(type = "string", description = "频道 ID", example = "12345") @JsonSerialize(using = ToStringSerializer.class) long cid,
        @Schema(type = "string", description = "申请用户 ID", example = "67890") @JsonSerialize(using = ToStringSerializer.class) long uid,
        @Schema(description = "申请理由", example = "hi") String reason,
        @Schema(description = "申请时间（epoch 毫秒）", example = "1700000000000") long applyTime,
        @Schema(description = "申请状态", example = "pending") String status
) {
}
