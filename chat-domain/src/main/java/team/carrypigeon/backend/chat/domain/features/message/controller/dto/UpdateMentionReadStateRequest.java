package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量更新提及已读状态请求。
 */
public record UpdateMentionReadStateRequest(
        @Schema(description = "标记该提及及之前的提及为已读", example = "723155640365318144") String beforeMentionId,
        @Schema(description = "只标记该频道内的提及", example = "12345") String cid
) {
}
