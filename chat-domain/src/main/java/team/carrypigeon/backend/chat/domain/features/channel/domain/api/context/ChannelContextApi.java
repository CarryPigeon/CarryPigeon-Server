package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;

/**
 * 频道上下文与成员查询领域 API。
 * 职责：提供账号在频道中的上下文，以及频道成员与治理账号的账号快照。
 * 边界：不暴露成员角色矩阵和治理规则细节，不为撤回、删除、置顶等具体业务动作定制校验方法，不暴露仓储实现。
 * 输入：频道 ID、账号 ID 等稳定业务标识。
 * 输出：频道成员上下文，以及成员账号、治理账号快照列表。
 * 失败语义：频道不存在与账号非成员关系由领域问题异常表达。
 * 调用方：message、file、server feature 通过本接口读取频道上下文，不直接依赖频道仓储。
 */
public interface ChannelContextApi {

    /**
     * 校验频道存在且账号是该频道成员，并返回该账号的频道上下文。
     * 输入：目标频道 ID 与待校验账号 ID。
     * 输出：含调用者角色与禁言状态的频道上下文。
     * 失败语义：频道不存在抛资源不存在；账号非成员抛权限不足。
     *
     * @param channelId 目标频道 ID
     * @param accountId 待校验的账号 ID
     * @return 该账号在目标频道的成员上下文
     */
    ChannelContext requireMemberContext(long channelId, long accountId);

    /**
     * 返回频道内全部成员的账号快照。
     * 输出：频道成员账号列表，用于消息广播与 mention 合法性校验。
     * 约束：不校验调用方身份，调用方应先通过 {@link #requireMemberContext} 完成鉴权。
     *
     * @param channelId 目标频道 ID
     * @return 频道内全部成员的账号 ID 快照
     */
    List<Long> memberAccountIds(long channelId);

    /**
     * 返回频道内具备治理资格的账号快照。
     * 输出：OWNER 与 ADMIN 的账号 ID 列表。
     * 用途：调用方据此判断某成员的内容是否可被治理，具体动作由调用方决定。
     *
     * @param channelId 目标频道 ID
     * @return 频道内 OWNER 与 ADMIN 的账号 ID 快照
     */
    List<Long> moderatorAccounts(long channelId);
}
