package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道禁言列表响应。
 * 职责：承载指定频道的禁言记录列表外壳。
 * 边界：只表达列表结构，不承载分页与实时语义。
 */
public record ChannelBanListResponse(
        @Schema(description = "禁言记录列表") List<ChannelBanListItemResponse> items
) {
}
