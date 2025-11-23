package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 用于更新频道成员信息的Node<br/>
 * 入参: <br/>
 * 1.ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 2.ChannelMemberInfo_Name:String?<br/>
 * 3.ChannelMemberInfo_Authority:int?<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberUpdater")
public class CPChannelMemberUpdaterNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember channelMemberInfo = context.getData("ChannelMemberInfo");
        if (channelMemberInfo == null){
            argsError(context);
        }
        String channelMemberInfoName = context.getData("ChannelMemberInfo_Name");
        if (channelMemberInfoName != null){
            channelMemberInfo.setName(channelMemberInfoName);
        }
        Integer channelMemberInfoAuthority = context.getData("ChannelMemberInfo_Authority");
        if (channelMemberInfoAuthority != null){
            channelMemberInfo.setAuthority(CPChannelMemberAuthorityEnum.valueOf(channelMemberInfoAuthority));
        }
        if (!channelMemberDao.save(channelMemberInfo)){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("update channel member error"));
            throw new CPReturnException();
        }
    }
}
