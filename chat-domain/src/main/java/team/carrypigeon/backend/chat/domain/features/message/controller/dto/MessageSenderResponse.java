package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息发送者快照。
 */
public record MessageSenderResponse(
        @Schema(description = "发送者用户 ID", example = "1001")
        String uid,
        @Schema(description = "发送者昵称", example = "Alice")
        String nickname,
        @Schema(description = "发送者头像相对路径", example = "avatars/u/1001.png")
        String avatar
) {
}
