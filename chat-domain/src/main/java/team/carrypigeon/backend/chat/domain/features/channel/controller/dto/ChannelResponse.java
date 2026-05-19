package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道协议响应。
 * 职责：向 HTTP 调用方暴露默认频道查询结果。
 * 边界：不承载业务规则与统一响应包装逻辑。
 *
 * @param channelId 频道 ID
 * @param conversationId 会话 ID
 * @param name 频道名称
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelResponse(
        @Schema(description = "频道 ID", example = "2001")
        long channelId,
        @Schema(description = "会话 ID", example = "3001")
        long conversationId,
        @Schema(description = "频道名称", example = "Project Phoenix")
        String name,
        @Schema(description = "频道类型", example = "private")
        String type,
        @Schema(description = "是否为默认频道", example = "false")
        boolean defaultChannel,
        @Schema(description = "创建时间", example = "2026-05-01T08:00:00Z")
        Instant createdAt,
        @Schema(description = "更新时间", example = "2026-05-13T08:00:00Z")
        Instant updatedAt
) {
}
