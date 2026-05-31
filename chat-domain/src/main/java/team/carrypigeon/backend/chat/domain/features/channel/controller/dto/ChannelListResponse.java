package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道列表响应。
 * 职责：承载 `GET /api/channels` 的列表外壳。
 * 边界：只表达列表结构，不承载分页与实时语义。
 */
public record ChannelListResponse(
        @Schema(description = "频道列表")
        List<ChannelSummaryResponse> channels
) {
}
