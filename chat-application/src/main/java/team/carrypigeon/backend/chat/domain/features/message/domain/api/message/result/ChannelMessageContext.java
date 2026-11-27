package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result;

import java.util.List;

/**
 * 频道消息锚点上下文结果。
 * 职责：向协议层暴露锚点前后固定窗口的消息列表。
 * 边界：不承载游标或分页状态；客户端需要继续扩展时使用独立的方向查询。
 *
 * @param items 上下文消息，按消息 ID 倒序排列
 */
public record ChannelMessageContext(
        List<ChannelMessageResult> items
) {
}
