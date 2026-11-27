package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/** 单频道读状态。ID 为十进制字符串，计数沿用频道角标的显示规则。 */
public record ChannelReadSummaryResponse(
    @Schema(type = "string", description = "频道 ID") @JsonSerialize(using = ToStringSerializer.class) long cid,
    @Schema(type = "string", description = "当前账号 ID") @JsonSerialize(using = ToStringSerializer.class) long uid,
    @Schema(type = "string", description = "最后已读消息 ID，入群时频道为空且尚未推进时为 null", nullable = true) @JsonSerialize(using = ToStringSerializer.class) Long lastReadMid,
    @Schema(description = "有效未读消息数，范围为 0 至 10000，10000 表示至少一万条") long unreadCount
) {
}
