package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 提及列表项响应。
 */
public record MentionItemResponse(
        @Schema(description = "提及 ID", example = "723155640365318144") String mentionId,
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "消息 ID", example = "723155640365318144") String mid,
        @Schema(description = "来源用户 ID", example = "67890") String fromUid,
        MentionTargetResponse target,
        @Schema(description = "创建时间", example = "1700000000000") long createdAt,
        @Schema(description = "是否已读", example = "false") boolean read
) {
    /**
     * 提及列表项中的目标摘要。
     * 职责：表达当前 mention 指向的目标类型与目标账号。
     */
    public record MentionTargetResponse(
            @Schema(description = "目标类型", example = "user") String type,
            @Schema(description = "目标用户 ID", example = "123") String uid
    ) {
    }
}
