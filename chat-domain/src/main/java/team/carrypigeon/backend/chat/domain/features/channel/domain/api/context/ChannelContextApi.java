package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;

/**
 * 频道上下文与成员查询领域 API。
 * 职责：提供账号在频道中的上下文，以及频道成员与治理账号的账号快照。
 */
public interface ChannelContextApi {

    /**
     * 校验频道存在且账号是该频道成员，并返回该账号的频道上下文。
     *
     * @param channelId 目标频道 ID
     * @param accountId 待校验的账号 ID
     * @return 该账号在目标频道的成员上下文
     */
    ChannelContext requireMemberContext(long channelId, long accountId);

    /**
     * 返回频道内全部成员的账号快照。
     *
     * @param channelId 目标频道 ID
     * @return 频道内全部成员的账号 ID 快照
     */
    List<Long> memberAccountIds(long channelId);

    /**
     * 返回频道内具备治理资格的账号快照。
     *
     * @param channelId 目标频道 ID
     * @return 频道内 OWNER 与 ADMIN 的账号 ID 快照
     */
    List<Long> moderatorAccounts(long channelId);
}
