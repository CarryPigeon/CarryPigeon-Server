package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result;

import java.util.List;

/**
 * 频道消息游标分页结果。
 * 职责：向协议层暴露当前页消息、下一页游标和是否还有更多数据。
 * 边界：只承载统一分页结果，不承载协议包装或具体查询方向。
 *
 * @param items 当前页消息
 * @param nextCursor 下一页游标，可为空
 * @param hasMore 是否还有下一页
 */
public record ChannelMessagePage(
        List<ChannelMessageResult> items,
        Long nextCursor,
        boolean hasMore
) {
}
