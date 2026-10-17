package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.ListChannelBansQuery;

/**
 * 频道禁言领域 API。
 * 职责：暴露频道成员禁言、按绝对时间禁言、解除禁言与禁言台账查询能力。
 * 边界：不暴露 HTTP 协议、具体实现类与禁言仓储细节；不负责成员角色调整与入群申请。
 * 输入：禁言、按时间禁言、解除禁言命令与禁言列表查询对象。
 * 输出：单条禁言结果投影或禁言列表项投影列表。
 * 失败语义：频道或记录不存在抛资源不存在；权限不足抛权限不足；目标角色不允许或禁言已生效抛参数错误。
 * 调用方：channel controller 通过本接口执行禁言治理，不直接依赖具体实现类。
 */
public interface ChannelBanApi {

    /**
     * 查询频道禁言台账。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色；返回结果包含已解除与已过期的历史记录。
     *
     * @param query 禁言列表查询条件
     * @return 禁言列表项投影列表
     */
    List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query);

    /**
     * 按持续时长对频道成员执行禁言。
     * 约束：调用方必须具备 OWNER 或 ADMIN 角色；目标成员角色必须为 MEMBER；已生效的禁言不允许重复设置。
     * 副作用：新建或覆盖该成员的禁言记录，并在事务提交后通知频道成员与目标账号。
     *
     * @param command 频道成员禁言命令
     * @return 禁言结果投影
     */
    ChannelBanResult banChannelMember(BanChannelMemberCommand command);

    /**
     * 按绝对截止时间对频道成员执行禁言。
     * 约束：截止时间必须晚于当前时间；内部换算为持续时长后复用按时长禁言的用例。
     *
     * @param command 按绝对过期时间的禁言命令
     * @return 禁言结果投影
     */
    ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command);

    /**
     * 解除频道成员的禁言。
     * 约束：调用方必须具备 OWNER 或 ADMIN 角色；目标必须存在仍然生效的禁言记录。
     * 副作用：标记禁言记录为已解除，并在事务提交后通知频道成员与目标账号。
     *
     * @param command 解除频道成员禁言命令
     * @return 解除后的禁言结果投影
     */
    ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command);
}
