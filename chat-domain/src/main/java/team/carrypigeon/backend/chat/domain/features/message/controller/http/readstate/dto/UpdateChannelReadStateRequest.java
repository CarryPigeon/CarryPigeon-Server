package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 更新频道已读状态请求。
 */
public record UpdateChannelReadStateRequest(
        @Schema(description = "最后已读消息 ID（十进制字符串）", type = "string", example = "100")
        @NotNull(message = "last_read_mid must not be null")
        @Positive(message = "last_read_mid must be greater than 0")
        Long lastReadMid,
        @Schema(description = "最后已读时间（epoch 毫秒）", example = "1700000000000")
        @Positive(message = "last_read_time must be greater than 0")
        long lastReadTime
) {
}
