package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 用于检查用户是否为频道管理员的Node<br/>
 * 入参: <br/>
 * 1.ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 2.UserInfo_Id:Long<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelAdminChecker")
public class CPChannelAdminChecker extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember channelMemberInfo = context.getData("ChannelMemberInfo");
        Long userInfoId = context.getData("UserInfo_Id");
        if (channelMemberInfo == null|| userInfoId == null){
            argsError(context);
        }
        if (channelMemberInfo.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the admin of this channel"));
            throw new CPReturnException();
        }
    }
}
