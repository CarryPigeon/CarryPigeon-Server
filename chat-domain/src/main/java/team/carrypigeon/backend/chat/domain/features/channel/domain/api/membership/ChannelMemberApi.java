package team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result.ChannelOwnershipTransferResult;

/**
 * 频道成员治理领域 API。
 * 职责：暴露成员查询、角色调整、踢出和所有权转移能力。
 * 边界：不负责频道禁言和频道申请流程。
 */
public interface ChannelMemberApi {

    List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query);

    ChannelMemberResult promoteChannelMember(PromoteChannelMemberCommand command);

    ChannelMemberResult demoteChannelAdmin(DemoteChannelAdminCommand command);

    ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command);

    void kickChannelMember(KickChannelMemberCommand command);
}
