package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * Guard for endpoints that require channel owner or admin role.
 * <p>
 * Requires {@link CPNodeChannelKeys#CHANNEL_INFO} (owner id) and {@link CPFlowKeys#SESSION_UID}.
 * <p>
 * If current user is not owner, requires {@link CPNodeChannelMemberKeys#CHANNEL_MEMBER_INFO} to check admin authority.
 */
@Slf4j
@LiteflowComponent("ApiChannelAdminOrOwnerGuard")
public class ApiChannelAdminOrOwnerGuardNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        CPChannel channel = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        if (uid == channel.getOwner()) {
            return;
        }
        CPChannelMember member = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        if (member.getUid() == channel.getOwner()) {
            return;
        }
        if (member.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("ApiChannelAdminOrOwnerGuard forbidden, uid={}, cid={}", uid, channel.getId());
            fail(CPProblem.of(403, "not_channel_admin", "channel admin required"));
        }
    }
}
