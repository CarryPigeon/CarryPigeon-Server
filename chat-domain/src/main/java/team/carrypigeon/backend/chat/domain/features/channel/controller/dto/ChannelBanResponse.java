package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道封禁响应。
 * 职责：向 HTTP 协议层返回频道封禁/解封动作后的稳定字段。
 * 边界：只承载协议输出，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被封禁账户 ID
 * @param operatorAccountId 操作人账户 ID
 * @param reason 封禁原因
 * @param expiresAt 到期时间
 * @param createdAt 创建时间
 * @param revokedAt 解封时间
 */
public record ChannelBanResponse(
        @Schema(description = "频道 ID", example = "2001")
        long channelId,
        @Schema(description = "被封禁账户 ID", example = "1002")
        long bannedAccountId,
        @Schema(description = "执行操作的账户 ID", example = "1001")
        long operatorAccountId,
        @Schema(description = "封禁原因", example = "spam messages")
        String reason,
        @Schema(description = "到期时间；为空表示无限期", example = "2026-05-14T08:00:00Z")
        Instant expiresAt,
        @Schema(description = "封禁创建时间", example = "2026-05-13T08:00:00Z")
        Instant createdAt,
        @Schema(description = "解除封禁时间；未解除时为空", example = "2026-05-13T12:00:00Z")
        Instant revokedAt
) {
}
