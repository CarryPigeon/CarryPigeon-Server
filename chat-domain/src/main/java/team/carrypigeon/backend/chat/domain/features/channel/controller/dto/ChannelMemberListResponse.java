package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道成员列表响应。
 * 职责：承载 `GET /api/channels/{cid}/members` 的 items 外壳。
 * 边界：只表达列表结构，不承载分页语义。
 */
public record ChannelMemberListResponse(
        @Schema(description = "成员列表")
        List<ChannelMemberV1Response> items
) {
}
