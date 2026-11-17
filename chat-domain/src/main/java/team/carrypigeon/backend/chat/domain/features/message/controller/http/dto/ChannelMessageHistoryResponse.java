package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;

/**
 * 消息方向查询响应。
 * @param items 当前页消息，latest/before 按频道序号降序，after 按升序
 * @param nextCursor 当前方向下一页游标，无更多消息时为空
 * @param hasMore 当前方向是否还有更多消息
 * @param syncCursor 用于 after 补拉的水位；latest 取本页最高序号，after 取本页末尾或原水位，before 为空
 *                   空频道的水位为空，调用 after 时省略 cursor 即从头补拉
 */
public record ChannelMessageHistoryResponse(
        List<ChannelMessageResponse> items,
        String nextCursor,
        boolean hasMore,
        String syncCursor
) {
}
