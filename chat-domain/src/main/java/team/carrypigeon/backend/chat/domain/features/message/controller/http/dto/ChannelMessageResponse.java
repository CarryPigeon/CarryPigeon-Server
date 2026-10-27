package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * canonical 消息响应。
 * 职责：把 canonical 领域消息无损转换为统一 HTTP 消息结构。
 * 边界：只包含统一消息字段，domain 专属字段全部位于 data；不解析或重建 data。
 */
public record ChannelMessageResponse(
        @Schema(description = "消息 ID", example = "1") String mid
) {

    /**
     * 把 canonical 消息结果映射为 v1 响应。
     *
     * @param result canonical 消息结果
     * @return v1 消息响应
     */
    public static ChannelMessageResponse from(ChannelMessageResult result) {
        return new ChannelMessageResponse(
                IdUtil.toString(result.messageId())
        );
    }
}
