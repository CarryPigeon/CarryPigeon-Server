package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道成员响应。
 * 职责：向 HTTP 协议层返回成员列表的稳定字段。
 * 边界：只承载协议输出，不承载成员治理逻辑。
 *
 * @param accountId 成员账户 ID
 * @param nickname 成员昵称
 * @param avatarUrl 成员头像地址
 * @param role 固定角色
 * @param joinedAt 加入时间
 * @param mutedUntil 禁言截止时间
 */
public record ChannelMemberResponse(
        @Schema(description = "成员账户 ID", example = "1002")
        long accountId,
        @Schema(description = "成员昵称", example = "Carry Pigeon")
        String nickname,
        @Schema(description = "成员头像地址", example = "https://cdn.example.com/avatar.png")
        String avatarUrl,
        @Schema(description = "频道角色", example = "ADMIN")
        String role,
        @Schema(description = "加入频道时间", example = "2026-05-01T08:00:00Z")
        Instant joinedAt,
        @Schema(description = "禁言截止时间；未禁言时为空", example = "2026-05-13T10:00:00Z")
        Instant mutedUntil
) {
}
