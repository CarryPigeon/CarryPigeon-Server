package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审计日志响应项。
 */
public record AuditLogItemResponse(
        @Schema(description = "审计记录 ID", example = "723155640365318144") String auditId,
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "操作者 UID", example = "67890") String actorUid,
        @Schema(description = "动作类型", example = "channel.create") String action,
        @Schema(description = "扩展细节 JSON", example = "{\"target\":\"1002\"}") String details,
        @Schema(description = "创建时间", example = "1700000000000") long createdAt
) {
}
