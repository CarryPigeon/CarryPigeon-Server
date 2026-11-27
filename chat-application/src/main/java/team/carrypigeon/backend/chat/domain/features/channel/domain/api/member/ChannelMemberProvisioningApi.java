package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member;

import java.time.Instant;

/**
 * 新账号频道成员关系初始化 API。
 * 职责：为新建账号建立全部系统频道的成员关系，使其登录后即可看到默认频道。
 */
public interface ChannelMemberProvisioningApi {

    /**
     * 初始化新账号的基础频道成员关系。
     * @param accountId 账号 ID
     * @param joinedAt 加入时间
     */
    void initializeMemberships(long accountId, Instant joinedAt);
}
