package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * 批量更新提及已读状态请求。
 */
public record UpdateMentionReadStateRequest(
        @Schema(description = "标记该提及及之前的提及为已读（十进制字符串）", type = "string", example = "723155640365318144")
        @Positive(message = "before_mention_id must be greater than 0")
        Long beforeMentionId,
        @Schema(description = "只标记该频道内的提及（十进制字符串）", type = "string", example = "12345")
        @Positive(message = "cid must be greater than 0")
        Long cid
) {
}
