package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.PositiveMessageApiIdDeserializer;

/** 范围已读的可选请求体。一旦提供对象，就必须给出包含端点的 ID 上界。 */
public record UpdateMentionReadStateRequest(
    @Schema(description = "标记该 ID 及之前的提及", type = "string", example = "4002")
    @NotNull
    @JsonDeserialize(using = PositiveMessageApiIdDeserializer.class)
    Long throughMentionId
) {
    /** 防止旧客户端的 cid 等限制条件被忽略后意外扩大更新范围。 */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Unknown mention read condition: " + field);
    }
}
