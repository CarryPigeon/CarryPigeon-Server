package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 用于设置通道成员权限的Node<br/>
 * bind: String(member|admin)<br/>
 * 入参: ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelMemberAuthoritySetter")
public class CPChannelMemberAuthoritySetterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember channelMember = context.getData("ChannelMemberInfo");
        String authority = getBindData("key",String.class);
        if (channelMember == null|| authority == null){
            argsError(context);
        }
        assert channelMember != null;
        switch (authority){
            case "member":
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
