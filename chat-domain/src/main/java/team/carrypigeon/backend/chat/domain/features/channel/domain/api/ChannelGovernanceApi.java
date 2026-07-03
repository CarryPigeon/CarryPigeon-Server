package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelOwnershipTransferResult;

/**
 * 频道治理领域 API。
 * 职责：暴露成员角色、禁言、踢出、封禁和所有权转移能力。
 * 边界：不暴露 controller 协议、具体实现类和审计持久化细节。
 * 输入：频道治理命令对象。
 * 输出：成员、封禁或所有权转移投影；无返回方法以治理副作用为结果。
 * 失败语义：权限不足、目标角色不可操作、频道类型不匹配和状态冲突由领域问题异常表达。
 * 调用方：只通过本接口触发治理动作，不直接写成员、封禁或审计存储。
 */
public interface ChannelGovernanceApi {

    /**
     * 将普通成员提升为频道管理员。
     * 输入：命令携带操作者、频道和目标成员。
     * 输出：提升后的频道成员投影。
     * 约束：只有满足角色层级规则的操作者可以提升成员。
     *
     * @param command 提升频道管理员业务命令
     * @return 提升后的频道成员投影
     */
    ChannelMemberResult promoteChannelMember(PromoteChannelMemberCommand command);

    /**
     * 将频道管理员降级为普通成员。
     * 输入：命令携带操作者、频道和目标管理员。
     * 输出：降级后的频道成员投影。
     * 约束：所有者和角色层级规则由领域治理策略校验。
     *
     * @param command 降级频道管理员业务命令
     * @return 降级后的频道成员投影
     */
    ChannelMemberResult demoteChannelAdmin(DemoteChannelAdminCommand command);

    /**
     * 转移频道所有权。
     * 输入：命令携带当前所有者、目标频道和新所有者账号。
     * 输出：所有权转移结果投影。
     * 副作用：更新原所有者和新所有者的频道成员角色。
     *
     * @param command 频道所有权转移业务命令
     * @return 所有权转移结果投影
     */
    ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command);

    /**
     * 禁言频道成员。
     * 输入：命令携带操作者、频道、目标成员和禁言期限。
     * 输出：禁言后的频道成员投影。
     * 约束：频道类型、操作者角色和目标角色必须满足治理规则。
     *
     * @param command 频道成员禁言业务命令
     * @return 禁言后的频道成员投影
     */
    ChannelMemberResult muteChannelMember(MuteChannelMemberCommand command);

    /**
     * 解除频道成员禁言。
     * 输入：命令携带操作者、频道和目标成员。
     * 输出：解除禁言后的频道成员投影。
     * 副作用：清除目标成员的禁言期限。
     *
     * @param command 频道成员解除禁言业务命令
     * @return 解除禁言后的频道成员投影
     */
    ChannelMemberResult unmuteChannelMember(UnmuteChannelMemberCommand command);

    /**
     * 将成员移出频道。
     * 输入：命令携带操作者、频道和目标成员。
     * 副作用：删除或失效目标成员关系，并记录治理审计。
     * 约束：目标成员角色不能高于或等于操作者可治理层级。
     *
     * @param command 踢出频道成员业务命令
     */
    void kickChannelMember(KickChannelMemberCommand command);

    /**
     * 封禁频道成员。
     * 输入：命令携带操作者、频道、目标账号和封禁原因。
     * 输出：创建或更新后的频道封禁投影。
     * 副作用：目标账号将不能继续作为普通成员参与该频道。
     *
     * @param command 频道成员封禁业务命令
     * @return 封禁结果投影
     */
    ChannelBanResult banChannelMember(BanChannelMemberCommand command);

    /**
     * 按截止时间封禁频道成员。
     * 输入：命令携带操作者、频道、目标账号和封禁截止时间。
     * 输出：创建或更新后的频道封禁投影。
     * 约束：封禁截止时间必须满足领域时间规则。
     *
     * @param command 限时封禁频道成员业务命令
     * @return 限时封禁结果投影
     */
    ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command);

    /**
     * 解除频道封禁。
     * 输入：命令携带操作者、频道和目标账号。
     * 输出：解除后的频道封禁投影。
     * 副作用：目标账号恢复申请或加入频道的可能性，但不自动成为成员。
     *
     * @param command 解除频道封禁业务命令
     * @return 解除封禁结果投影
     */
    ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command);
}
