package team.carrypigeon.backend.chat.domain.features.message.domain.api.mention;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionPage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionResult;

/** 当前账号的提及收件箱，只允许读取和处理本人在已加入频道内的提及。 */
public interface MentionApi {

    /** 查询一条可见提及，缺失或不可见均返回提及不存在。 */
    MentionResult getMention(long accountId, long mentionId);

    /** 按输入首次出现的顺序返回可见提及，略过缺失项。 */
    List<MentionResult> getMentions(long accountId, List<Long> mentionIds);

    /** 跨频道按提及 ID 倒序读取一页，查询本身不标记已读。 */
    MentionPage listMentions(ListMentionsQuery query);

    /** 查询指定频道的收件箱，账号必须是当前成员。 */
    MentionPage listChannelMentions(long channelId, ListMentionsQuery query);

    /** 单条幂等已读；不存在或不再可见的提及统一报错。 */
    void markMentionRead(long accountId, long mentionId);

    /** 标记所有可见频道内的提及；上界包含端点，null 表示本次更新匹配的全部。 */
    void markMentionsRead(long accountId, Long throughMentionId);

    /** 仅标记指定频道内的提及，成员要求和上界含义同收件箱查询与个人范围已读。 */
    void markChannelMentionsRead(long accountId, long channelId, Long throughMentionId);
}
