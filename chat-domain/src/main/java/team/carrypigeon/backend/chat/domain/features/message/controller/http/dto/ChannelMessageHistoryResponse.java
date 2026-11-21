package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;

/**
 * 消息方向查询响应。
 * @param items 当前页消息，latest/before 按频道序号降序，after 按升序
 * @param nextCursor 当前方向下一页游标，无更多消息时为空
 * @param hasMore 当前方向是否还有更多消息
 */
public record ChannelMessageHistoryResponse(
        List<ChannelMessageResponse> items,
        String nextCursor,
        boolean hasMore
) {
}
