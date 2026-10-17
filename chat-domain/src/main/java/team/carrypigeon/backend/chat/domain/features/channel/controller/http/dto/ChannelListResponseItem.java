package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道摘要响应项。
 * 职责：承载频道列表与创建频道返回所需的频道标识、名称与简介。
 * 边界：只表达列表渲染所需的最小字段，不含头像、系统频道标记与时间戳；频道详情请使用 ChannelQueryResponse。
 */
public record ChannelListResponseItem(
        @Schema(description = "频道 ID", example = "1")
        String cid,
        @Schema(description = "频道名称", example = "General")
        String name,
        @Schema(description = "频道简介", example = "")
        String brief
) {
}
