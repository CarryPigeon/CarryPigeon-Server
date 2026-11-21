package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.PositiveMessageApiIdDeserializer;

/**
 * 更新频道已读状态请求。
 */
public record UpdateChannelReadStateRequest(
        @Schema(description = "最后已读消息 ID（十进制字符串）", type = "string", example = "100")
        @NotNull(message = "last_read_mid must not be null")
        @JsonDeserialize(using = PositiveMessageApiIdDeserializer.class)
        Long lastReadMid
) {
}
