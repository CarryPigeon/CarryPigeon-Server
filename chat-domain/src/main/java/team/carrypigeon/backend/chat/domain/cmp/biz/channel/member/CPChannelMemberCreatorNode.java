package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
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
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channelInfo = context.getData(CPNodeChannelKeys.CHANNEL_INFO);
        Long userId = context.getData(CPNodeUserKeys.USER_INFO_ID);
        if (channelInfo == null|| userId == null){
            argsError(context);
        }
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
