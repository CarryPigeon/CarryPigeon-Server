package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道摘要响应。
 * 职责：承载频道列表与按 ID 查询所需的最小公共字段。
 * 边界：当前只表达 v1 已落地的 HTTP 字段，不扩展频道资料完整模型。
 */
public record ChannelQueryResponse(
    @Schema(description = "频道 ID",example = "123456")
    String cid,
    @Schema(description = "频道名称",example = "技术交流")
    String name,
    @Schema(description = "频道简介",example = "一个关于技术交流的频道")
    String brief,
    /*@Schema(description = "频道头像",example = "https://example.com/avatar.jpg")
    String avatar,*/
    @Schema(description = "是否为默认频道",example = "true")
    boolean systemChannel,
    @Schema(description = "创建时间",example = "2023-01-01T00:00:00Z")
    Long createdAt
) {
}
