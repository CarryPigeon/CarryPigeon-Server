package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

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
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelMember channelMemberInfo = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        String channelMemberInfoName = context.get(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_NAME);
        if (channelMemberInfoName != null){
            channelMemberInfo.setName(channelMemberInfoName);
        }
        Integer channelMemberInfoAuthority = context.get(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_AUTHORITY);
        if (channelMemberInfoAuthority != null){
            channelMemberInfo.setAuthority(CPChannelMemberAuthorityEnum.valueOf(channelMemberInfoAuthority));
        }
        if (!channelMemberDao.save(channelMemberInfo)){
            fail(CPProblem.of(500, "internal_error", "update channel member error"));
        }
    }
}
