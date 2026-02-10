package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 频道管理员或所有者权限守卫节点。
 * <p>
 * 校验当前用户是否为频道 owner；若不是则要求其成员权限为 ADMIN。
 */
@Slf4j
@LiteflowComponent("ApiChannelAdminOrOwnerGuard")
public class ApiChannelAdminOrOwnerGuardNode extends CPNodeComponent {

    /**
     * 校验“owner 或 admin”权限。
     */
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
            fail(CPProblem.of(CPProblemReason.NOT_CHANNEL_ADMIN, "channel admin required"));
        }
    }
}
