package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAfterQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesBeforeQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetLatestChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;

/**
 * 频道消息方向查询领域 API。
 * 职责：分别暴露最新消息、更旧消息和更新消息三个游标查询。
 * 边界：不暴露锚点上下文或搜索；三种查询的方向由各自方法固定，不通过可选参数切换。
 */
public interface ChannelMessageHistoryApi {

    /**
     * 查询频道最新消息页。
     *
     * @param query 最新消息查询条件
     * @return 最新消息页，游标用于继续查询更旧消息
     */
    ChannelMessagePage getLatestMessages(GetLatestChannelMessagesQuery query);

    /**
     * 查询指定游标之前的更旧消息页。
     *
     * @param query 更旧消息查询条件
     * @return 更旧消息页
     */
    ChannelMessagePage getMessagesBefore(GetChannelMessagesBeforeQuery query);

    /**
     * 查询指定游标之后的更新消息页。
     *
     * @param query 更新消息查询条件
     * @return 更新消息页
     */
    ChannelMessagePage getMessagesAfter(GetChannelMessagesAfterQuery query);
}
