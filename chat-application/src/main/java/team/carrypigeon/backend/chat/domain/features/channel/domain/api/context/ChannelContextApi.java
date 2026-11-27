package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;

/**
 * 频道上下文与成员查询领域 API。
 * 职责：提供账号的频道上下文、有限候选成员过滤和在线订阅所需的频道关系。
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

    /** 消息等频道写入先锁频道再校验成员，锁持有到外层事务结束，避免删除后创建新的关联数据。 */
    ChannelContext requireMemberContextForUpdate(long channelId, long accountId);

    /** 返回账号已加入且尚未删除的频道 ID，不使用展示缓存。 */
    List<Long> joinedChannelIds(long accountId);

    /** 查询目标当前是否为频道治理者，不要求目标仍是成员；非成员返回 false。 */
    boolean isModerator(long channelId, long accountId);

    /** 发送事务持有频道锁时，取候选账号中当前仍属该频道的成员 ID，去重并按 ID 升序返回。 */
    List<Long> findMemberAccountIds(long channelId, List<Long> candidates);
}
