package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/** 按请求顺序读取消息，ID 支持整数或十进制字符串。 */
public record ChannelMessageIdsRequest(
    @ArraySchema(schema = @Schema(oneOf = {String.class, Long.class}))
    @NotNull List<@NotNull @Positive Long> messageIds
) {
}
