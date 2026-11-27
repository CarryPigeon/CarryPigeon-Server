package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;

/**
 * 频道消息历史响应。
 * @param items 按消息 ID 降序排列的当前页消息
 * @param hasMore 是否还有更旧的消息
 */
public record ChannelMessageHistoryResponse(
        List<ChannelMessageResponse> items,
        boolean hasMore
) {
}
