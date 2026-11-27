package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.ListChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;

/** 当前成员的频道消息历史查询。 */
public interface ChannelMessageHistoryApi {

    /**
     * 按消息 ID 降序读取一页。指定上界时只返回严格小于该 ID 的消息。
     * 频道不存在或账号不是当前成员时按频道访问规则返回业务错误。
     */
    ChannelMessagePage listMessages(long accountId, ListChannelMessagesQuery query);
}
