package team.carrypigeon.backend.chat.domain.features.channel.controller.http.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 频道成员列表响应。
 * 职责：承载指定频道成员列表的 items 外壳。
 * 边界：只表达列表结构，不承载分页游标与总数聚合；成员资料缺失时由列表项内部字段体现。
 */
public record ChannelMemberListResponse(
        @Schema(description = "成员列表")
        List<ChannelMemberResponse> items
) {
}
