package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 按 ID 读取提及。条数限制按原始输入计算，重复 ID 在领域层合并。 */
public record MentionIdsRequest(
    @ArraySchema(schema = @Schema(oneOf = {String.class, Long.class}, example = "4001"), minItems = 1, maxItems = 50)
    @NotNull
    @Size(min = 1, max = 50)
    List<@NotNull @Positive Long> mentionIds
) {
}
