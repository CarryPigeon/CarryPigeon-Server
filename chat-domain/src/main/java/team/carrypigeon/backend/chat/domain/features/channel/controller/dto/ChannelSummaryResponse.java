package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道摘要响应。
 * 职责：承载频道列表与按 ID 查询所需的最小公共字段。
 * 边界：当前只表达 v1 已落地的 HTTP 字段，不扩展频道资料完整模型。
 */
public record ChannelSummaryResponse(
        @Schema(description = "频道 ID", example = "1")
        String cid,
        @Schema(description = "频道名称", example = "General")
        String name,
        @Schema(description = "频道简介", example = "")
        String brief,
        @Schema(description = "频道头像相对路径", example = "")
        String avatar,
        @Schema(description = "频道 owner 用户 ID", example = "1001")
        String ownerUid
) {
}
