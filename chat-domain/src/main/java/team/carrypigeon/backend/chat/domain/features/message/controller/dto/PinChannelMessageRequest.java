package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 置顶消息请求。
 */
public record PinChannelMessageRequest(
        @Schema(description = "置顶备注", example = "重要通知")
        String note
) {
}
