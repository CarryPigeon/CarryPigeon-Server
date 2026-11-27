package team.carrypigeon.backend.chat.domain.features.channel.controller.http.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道成员响应项。
 * 职责：承载成员列表所需的账户身份、角色、头像与加入时间。
 * 边界：只暴露成员展示字段，不暴露成员关系主键与内部治理状态。
 */
public record ChannelMemberResponse(
        @Schema(description = "用户 ID", example = "1001")
        String uid,
        @Schema(description = "用户名（同时作为公开昵称）", example = "Alice")
        String username,
        @Schema(description = "频道角色", example = "owner")
        String role,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
        String avatar,
        @Schema(description = "加入时间（epoch 毫秒）", example = "1700000000000")
        Instant joinTime
) {
}
