package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道邀请响应。
 * 职责：向 HTTP 协议层返回邀请记录的稳定字段。
 * 边界：只承载协议输出，不承载邀请业务逻辑。
 *
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间
 */
public record ChannelInviteResponse(
        @Schema(description = "频道 ID", example = "2001")
        long channelId,
        @Schema(description = "被邀请账户 ID", example = "1002")
        long inviteeAccountId,
        @Schema(description = "邀请发起人账户 ID", example = "1001")
        long inviterAccountId,
        @Schema(description = "邀请状态", example = "PENDING")
        String status,
        @Schema(description = "邀请创建时间", example = "2026-05-13T08:00:00Z")
        Instant createdAt,
        @Schema(description = "响应时间；未响应时为空", example = "2026-05-13T09:00:00Z")
        Instant respondedAt
) {
}
