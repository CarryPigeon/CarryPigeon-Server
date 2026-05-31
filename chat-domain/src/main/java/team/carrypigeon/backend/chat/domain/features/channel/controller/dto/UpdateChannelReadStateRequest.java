package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新频道已读状态请求。
 */
public record UpdateChannelReadStateRequest(
        @Schema(description = "最后已读消息 ID", example = "100")
        @NotBlank(message = "last_read_mid must not be blank")
        String lastReadMid,
        @Schema(description = "最后已读时间（epoch 毫秒）", example = "1700000000000")
        long lastReadTime
) {
}
