package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于创建通道成员的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel};UserInfo_Id:Long<br/>
 * 出参: ChannelMember:{@link CPChannelMember}<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelMemberCreator")
public class CPChannelMemberCreatorNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannel channelInfo = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO, CPChannel.class);
        Long userId = requireContext(context, CPNodeUserKeys.USER_INFO_ID, Long.class);
        CPChannelMember channelMemberInfo = new CPChannelMember();
        channelMemberInfo
                .setId(IdUtil.generateId())
                .setCid(channelInfo.getId())
                .setUid(userId)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(TimeUtil.getCurrentLocalTime());
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, channelMemberInfo);
    }
}
