package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道消息搜索协议响应。
 * 职责：向 HTTP 调用方暴露关键字搜索命中的消息列表。
 * 边界：不承载统一响应包装逻辑。
 *
 * @param messages 搜索命中消息列表
 */
public record ChannelMessageSearchResponse(
        @Schema(description = "关键字搜索命中的消息列表")
        List<ChannelMessageResponse> messages
) {
}
