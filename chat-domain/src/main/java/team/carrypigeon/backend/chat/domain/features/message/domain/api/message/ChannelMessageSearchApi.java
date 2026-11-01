package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;

/**
 * 频道消息搜索领域 API。
 * 职责：暴露频道内按关键字和过滤条件搜索消息的游标查询。
 * 边界：不承载时间线方向或锚点上下文。
 */
public interface ChannelMessageSearchApi {

    /**
     * 搜索频道消息。
     *
     * @param query 搜索查询条件
     * @return 搜索结果页
     */
    ChannelMessagePage searchMessages(SearchChannelMessagesQuery query);
}
