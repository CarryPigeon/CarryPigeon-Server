package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

/**
 * 频道禁言领域 API。
 * 职责：暴露按绝对时间禁言、解除禁言与禁言台账查询能力。
 * 边界：不暴露 HTTP 协议、具体实现类与禁言仓储细节；不负责成员角色调整与入群申请。
 * 输入：按时间禁言、解除禁言命令与禁言列表查询对象。
 * 输出：单条禁言结果投影或禁言列表项投影列表。
 * 失败语义：频道或记录不存在抛资源不存在；权限不足抛权限不足；目标角色不允许或禁言已生效抛参数错误。
 * 调用方：channel controller 通过本接口执行禁言治理，不直接依赖具体实现类。
 */
public interface ChannelBanApi {

    /**
     * 分页查询频道禁言台账。
     * @param accountId 账号 ID
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页禁言列表项投影
     */
    PageResult<ChannelBanListItemResult> listChannelBans(long accountId, long channelId, PageQuery page);

    /**
     * 按绝对截止时间对频道成员执行禁言。
     * 约束：截止时间必填且必须晚于当前时间；已有生效禁言时拒绝重复设置。
     *
     * @param command 按绝对过期时间的禁言命令
     * @return 禁言结果投影
     */
    ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command);

    /**
     * 解除频道成员的禁言。
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     * @param targetAccountId 目标账号 ID
     */
    void unbanChannelMember(long operatorAccountId, long channelId, long targetAccountId);
}
