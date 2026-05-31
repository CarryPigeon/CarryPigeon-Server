package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 入群申请列表响应。
 */
public record ChannelApplicationListResponse(
        @Schema(description = "申请列表") List<ChannelApplicationResponse> items
) {
}
