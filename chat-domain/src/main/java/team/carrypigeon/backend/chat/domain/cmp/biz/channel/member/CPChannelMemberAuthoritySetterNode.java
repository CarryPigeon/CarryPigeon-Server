package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 用于设置通道成员权限的Node<br/>
 * bind: String(member|admin)<br/>
 * 入参: session_uid:Long;ChannelInfo:{@link CPChannel};ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelMemberAuthoritySetter")
public class CPChannelMemberAuthoritySetterNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelMember channelMember = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        CPChannel channelInfo = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        String authority = requireBind(CPNodeBindKeys.KEY, String.class);
        Long sessionUid = requireContext(context, CPFlowKeys.SESSION_UID);

        // Only channel owner can change member authority (chain should also guard, but node keeps the invariant).
        if (channelInfo.getOwner() != sessionUid) {
            forbidden("not_channel_owner", "you are not the owner of this channel");
        }

        switch (authority) {
            case "member" -> {
                if (channelMember.getUid() == channelInfo.getOwner()) {
                    forbidden("cannot_change_owner_authority", "cannot change owner authority");
                }
                channelMember.setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
            }
            case "admin" -> channelMember.setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
            default -> validationFailed();
        }
    }
}
