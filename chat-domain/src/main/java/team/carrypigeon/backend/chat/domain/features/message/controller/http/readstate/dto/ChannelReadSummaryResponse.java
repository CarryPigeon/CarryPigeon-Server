package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 单频道读状态。ID 为十进制字符串，计数沿用频道角标的显示规则。 */
public record ChannelReadSummaryResponse(
    @Schema(description = "频道 ID") String cid,
    @Schema(description = "当前账号 ID") String uid,
    @Schema(description = "最后已读消息 ID，从未上报时为 null", nullable = true) String lastReadMid,
    @Schema(description = "有效未读消息数，范围为 0 至 10000，10000 表示至少一万条") long unreadCount
) {
}
