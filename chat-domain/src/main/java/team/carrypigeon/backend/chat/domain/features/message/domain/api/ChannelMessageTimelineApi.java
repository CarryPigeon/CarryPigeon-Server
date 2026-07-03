package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.SearchChannelMessagesQuery;

/**
 * 频道消息时间线领域 API。
 * 职责：暴露频道消息历史读取与搜索能力。
 * 边界：不暴露消息发布、编辑、撤回、删除、附件上传和置顶能力。
 * 输入：消息历史和搜索查询对象。
 * 输出：消息历史页或搜索结果投影。
 * 失败语义：频道不存在、成员权限不足、游标非法和搜索条件非法由领域问题异常表达。
 * 调用方：controller 或其它 feature 通过本接口读取消息时间线，不直接读取消息仓储。
 */
public interface ChannelMessageTimelineApi {

    /**
     * 查询频道历史消息。
     * 输入：查询对象携带账号、频道、游标、around 消息和分页数量。
     * 输出：按时间线规则返回的消息列表和下一页游标。
     * 约束：调用方不应假设底层排序或分页实现，只消费领域结果投影。
     *
     * @param query 频道历史消息查询条件
     * @return 频道消息历史结果投影
     */
    ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query);

    /**
     * 在频道内搜索消息。
     * 输入：查询对象携带账号、频道、关键字、发送者、消息领域和游标条件。
     * 输出：匹配条件的频道消息搜索结果投影。
     * 约束：撤回消息、权限和领域名归一化由领域实现统一处理。
     *
     * @param query 频道消息搜索查询条件
     * @return 频道消息搜索结果投影
     */
    ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query);
}
