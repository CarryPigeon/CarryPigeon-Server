package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelOwnershipTransferResult;

/**
 * 频道成员治理领域 API。
 * 职责：暴露频道成员列表读取、管理员角色调整、所有权转移与成员移除能力。
 * 边界：不暴露成员角色枚举与仓储细节；不负责频道生命周期管理，也不负责禁言与入群申请。
 * 输入：成员列表查询对象，以及提升、降级、转移与移除命令。
 * 输出：成员投影、所有权转移结果投影或无返回值的删除副作用。
 * 失败语义：频道或成员不存在抛资源不存在；权限不足抛权限不足；目标角色不符合前置条件抛参数错误。
 * 调用方：channel controller 通过本接口执行成员治理，不直接依赖具体实现类。
 */
public interface ChannelMemberApi {

    /**
     * 查询频道成员列表。
     * 约束：请求者必须是该频道成员。
     *
     * @param query 频道成员列表查询条件
     * @return 频道成员投影列表
     */
    List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query);

    /**
     * 将普通成员提升为频道管理员。
     * 约束：仅频道 OWNER 可执行；目标成员当前角色必须是 MEMBER。
     * 副作用：把目标成员角色改为 ADMIN，并在事务提交后通知频道成员集变化。
     *
     * @param command 成员提升命令
     */
    void promoteChannelMember(PromoteChannelMemberCommand command);

    /**
     * 将频道管理员降级为普通成员。
     * 约束：仅频道 OWNER 可执行；目标成员当前角色必须是 ADMIN。
     * 副作用：把目标成员角色改回 MEMBER，并在事务提交后通知频道成员集变化。
     *
     * @param command 管理员降级命令
     */
    void demoteChannelAdmin(DemoteChannelAdminCommand command);

    /**
     * 转移频道所有权。
     * 约束：仅现任 OWNER 可发起；目标不能已经是 OWNER；两次角色变更处于同一事务。
     * 副作用：原 OWNER 降为 ADMIN、目标升为 OWNER，并在事务提交后通知频道成员集变化。
     *
     * @param command 所有权转移命令
     * @return 所有权转移结果投影
     */
    ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command);

    /**
     * 将成员移出频道。
     * 约束：调用方必须具备 OWNER 或 ADMIN 角色；目标成员当前角色必须是 MEMBER。
     * 副作用：删除目标成员关系，并在事务提交后同时通知频道成员集变化与被移除账号刷新频道列表。
     *
     * @param command 踢出成员命令
     */
    void kickChannelMember(KickChannelMemberCommand command);
}
