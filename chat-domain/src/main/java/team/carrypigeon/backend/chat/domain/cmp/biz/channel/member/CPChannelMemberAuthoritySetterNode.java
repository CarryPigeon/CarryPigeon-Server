package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 用于设置通道成员权限的Node<br/>
 * bind: String(member|admin)<br/>
 * 入参: SessionId:Long;ChannelInfo:{@link CPChannel};ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelMemberAuthoritySetter")
public class CPChannelMemberAuthoritySetterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember channelMember = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO);
        CPChannel channelInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        String authority = getBindData("key", String.class);
        Long sessionId = context.getData(CPNodeValueKeyBasicConstants.SESSION_ID);
        if (channelMember == null|| authority == null|| channelInfo == null){
            argsError(context);
        }
        assert channelMember != null;
        switch (authority){
            case "member":
                if (channelInfo.getOwner() != sessionId){
                    context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                            CPResponse.AUTHORITY_ERROR_RESPONSE.setTextData("you are the owner of this channel"));
                    throw new CPReturnException();
                }
                channelMember.setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
                break;
            case "admin":
                channelMember.setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
                break;
            case null:
            default:
                argsError(context);
        }
    }
}
