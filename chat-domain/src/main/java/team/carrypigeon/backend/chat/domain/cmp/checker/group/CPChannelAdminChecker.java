package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;

/**
 * 用于检查用户是否为频道管理员的Node<br/>
 * 入参: <br/>
 * 1.ChannelInfo_Id:Long<br/>
 * 2.UserInfo_Id:Long<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelAdminChecker")
public class CPChannelAdminChecker extends CPNodeComponent {
    private final ChannelMemberDao channelMemberDao;
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long channelId = context.getData("ChannelInfo_Id");
        Long userInfoId = context.getData("UserInfo_Id");
        if (channelId == null|| userInfoId == null){
            argsError(context);
            return;
        }
        CPChannelMember channelMemberInfo = channelMemberDao.getMember(userInfoId, channelId);
        if (channelMemberInfo==null){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("you are not in this channel"));
            throw new CPReturnException();
        }
        if (channelMemberInfo.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the admin of this channel"));
            throw new CPReturnException();
        }
    }
}