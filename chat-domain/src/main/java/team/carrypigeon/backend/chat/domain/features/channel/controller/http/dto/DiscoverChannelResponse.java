package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道发现响应项。
 * 职责：承载频道发现列表所需的频道摘要、成员规模与加入门槛提示。
 * 边界：只服务发现读链路，不含频道完整资料与当前账号的成员状态。
 */
public record DiscoverChannelResponse(
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "频道名", example = "General") String name,
        @Schema(description = "频道简介", example = "讨论区") String brief,
        @Schema(description = "频道头像", example = "avatars/ch/12345.png") String avatar,
        @Schema(description = "成员数", example = "42") long memberCount,
        @Schema(description = "是否需要申请加入", example = "false") boolean requiresApplication
) {
}
