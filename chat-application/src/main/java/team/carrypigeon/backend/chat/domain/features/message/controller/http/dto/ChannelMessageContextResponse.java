package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道消息锚点上下文响应。
 * 职责：承载锚点前后固定窗口的消息列表。
 * 边界：不承载游标或分页状态。
 *
 * @param items 上下文消息
 */
public record ChannelMessageContextResponse(
        @Schema(description = "上下文消息列表")
        List<ChannelMessageResponse> items
) {
}
