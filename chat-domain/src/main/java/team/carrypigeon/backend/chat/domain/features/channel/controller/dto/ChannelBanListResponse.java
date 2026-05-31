package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道封禁列表响应。
 */
public record ChannelBanListResponse(
        @Schema(description = "封禁列表") List<ChannelBanListItemResponse> items
) {
}
