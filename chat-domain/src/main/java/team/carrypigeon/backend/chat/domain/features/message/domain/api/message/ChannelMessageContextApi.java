package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAroundQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageContext;

/**
 * 频道消息锚点上下文查询领域 API。
 * 职责：暴露以指定消息为中心的固定窗口查询。
 * 边界：不承载历史翻页或搜索；翻页由独立方向查询完成。
 */
public interface ChannelMessageContextApi {

    /**
     * 查询锚点消息前后的上下文窗口。
     *
     * @param query 锚点上下文查询条件
     * @return 锚点上下文消息
     */
    ChannelMessageContext getMessagesAround(GetChannelMessagesAroundQuery query);
}
