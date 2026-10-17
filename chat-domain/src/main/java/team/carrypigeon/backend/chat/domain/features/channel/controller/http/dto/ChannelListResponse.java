package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道列表响应。
 * 职责：承载当前账号可见频道列表的外壳。
 * 边界：只表达列表结构，不承载分页与实时语义；可见性由领域层按成员关系决定。
 */
public record ChannelListResponse(
        @Schema(description = "频道列表")
        List<ChannelListResponseItem> channels
) {
}
