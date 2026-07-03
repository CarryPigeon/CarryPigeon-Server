package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListMentionsQuery;

/**
 * 提及领域 API。
 * 职责：暴露提及列表和已读标记能力。
 * 边界：不暴露 controller 协议、具体实现类和 mention 仓储细节。
 * 输入：提及列表查询对象或账号、mention、频道游标等稳定业务入参。
 * 输出：提及投影列表或已读标记副作用。
 * 失败语义：账号非法、mention 不存在、频道不可访问和游标非法由领域问题异常表达。
 * 调用方：通过本接口读取和维护 mention 已读状态，不直接写 mention 存储。
 */
public interface MentionApi {

    /**
     * 查询账号收到的提及列表。
     * 输入：查询对象携带账号、频道筛选、未读筛选和分页游标。
     * 输出：提及结果投影列表。
     * 约束：列表只返回当前账号可见的提及记录。
     *
     * @param query 提及列表查询条件
     * @return 提及结果投影列表
     */
    List<MentionResult> listMentions(ListMentionsQuery query);

    /**
     * 将单条提及标记为已读。
     * 输入：账号 ID 和 mention ID。
     * 副作用：更新目标 mention 的已读状态。
     * 约束：账号只能标记归属于自己的 mention。
     *
     * @param accountId 当前账号 ID
     * @param mentionId 目标 mention ID
     */
    void markMentionRead(long accountId, long mentionId);

    /**
     * 批量标记提及为已读。
     * 输入：账号 ID、可选 mention 上界和可选频道 ID。
     * 副作用：将范围内属于当前账号的 mention 标记为已读。
     * 约束：范围过滤和频道可访问性由领域实现统一判断。
     *
     * @param accountId 当前账号 ID
     * @param beforeMentionId 可选 mention 上界，通常表示只处理该 ID 之前的提及
     * @param channelId 可选频道 ID，用于限制只处理某个频道内的提及
     */
    void markMentionsRead(long accountId, Long beforeMentionId, Long channelId);
}
