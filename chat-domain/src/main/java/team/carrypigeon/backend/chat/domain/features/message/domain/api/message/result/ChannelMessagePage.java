package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result;

import java.util.List;

/**
 * 频道消息分页结果。
 * @param items 按消息 ID 降序排列的当前页消息
 * @param hasMore 是否还有下一页
 */
public record ChannelMessagePage(
        List<ChannelMessageResult> items,
        boolean hasMore
) {
}
