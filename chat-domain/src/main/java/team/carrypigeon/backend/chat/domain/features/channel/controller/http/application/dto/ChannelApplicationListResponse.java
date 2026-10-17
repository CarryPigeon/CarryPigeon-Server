package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 入群申请列表响应。
 * 职责：承载指定频道的入群申请列表外壳。
 * 边界：只表达列表结构，不承载分页游标与总数聚合；空列表以空数组表达。
 */
public record ChannelApplicationListResponse(
        @Schema(description = "入群申请列表") List<ChannelApplicationResponse> items
) {
}
