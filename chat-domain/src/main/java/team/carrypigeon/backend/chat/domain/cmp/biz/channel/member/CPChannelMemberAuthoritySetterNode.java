package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;

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
    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param session 当前请求会话（用于识别操作者）
     * @param context LiteFlow 上下文，读取成员实体并更新权限值
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelMember channelMember = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        CPChannel channelInfo = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        String authority = requireBind(CPNodeBindKeys.KEY, String.class);
        Long sessionUid = requireContext(context, CPFlowKeys.SESSION_UID);
        if (channelInfo.getOwner() != sessionUid) {
            forbidden(CPProblemReason.NOT_CHANNEL_OWNER, "you are not the owner of this channel");
        }

        switch (authority) {
            case "member" -> {
                if (channelMember.getUid() == channelInfo.getOwner()) {
                    forbidden(CPProblemReason.CANNOT_CHANGE_OWNER_AUTHORITY, "cannot change owner authority");
                }
                channelMember.setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
            }
            case "admin" -> channelMember.setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
            default -> validationFailed();
        }
    }
}
