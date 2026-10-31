package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

/**
 * 频道成员治理领域 API。
 * 职责：暴露频道成员列表读取、管理员角色调整、所有权转移与成员移除能力。
 */
public interface ChannelMemberApi {

    /**
     * 分页查询频道成员列表。
     * @param accountId 账号 ID
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页成员投影
     */
    PageResult<ChannelMemberResult> listChannelMembers(long accountId, long channelId, PageQuery page);

    /**
     * 将普通成员提升为频道管理员。
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     * @param targetAccountId 目标账号 ID
     */
    void promoteChannelMember(long operatorAccountId, long channelId, long targetAccountId);

    /**
     * 将频道管理员降级为普通成员。
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     * @param targetAccountId 目标账号 ID
     */
    void demoteChannelAdmin(long operatorAccountId, long channelId, long targetAccountId);

    /**
     * 转移频道所有权。
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     * @param targetAccountId 目标账号 ID
     */
    ChannelOwnershipTransferResult transferChannelOwnership(long operatorAccountId, long channelId, long targetAccountId);

    /**
     * 将成员移出频道。
     * @param operatorAccountId 操作者账号 ID
      * @param channelId 频道 ID
     * @param targetAccountId 目标账号 ID
     */
    void kickChannelMember(long operatorAccountId, long channelId, long targetAccountId);
}
