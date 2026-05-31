package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 未读列表响应。
 */
public record UnreadListResponse(
        @Schema(description = "未读频道项") List<UnreadItemResponse> items
) {
}
