package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道成员 v1 响应。
 * 职责：承载成员列表最小公共字段。
 * 边界：不暴露旧协议的 accountId/avatarUrl/joinedAt 命名。
 */
public record ChannelMemberV1Response(
        @Schema(description = "用户 ID", example = "1001")
        String uid,
        @Schema(description = "频道角色", example = "owner")
        String role,
        @Schema(description = "用户昵称", example = "Alice")
        String nickname,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
        String avatar,
        @Schema(description = "加入时间（epoch 毫秒）", example = "1700000000000")
        Instant joinTime
) {
}
