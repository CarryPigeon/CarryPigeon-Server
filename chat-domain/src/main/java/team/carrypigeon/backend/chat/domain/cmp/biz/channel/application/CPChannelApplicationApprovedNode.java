package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 频道申请通过Node<br/>
 * 入参：ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelApplicationApproved")
public class CPChannelApplicationApprovedNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO);
        if (channelApplicationInfo == null){
            argsError(context);
            return;
        }
        CPChannelMember channelMemberInfo = new CPChannelMember();
        channelMemberInfo
                .setId(IdUtil.generateId())
                .setCid(channelApplicationInfo.getCid())
                .setUid(channelApplicationInfo.getUid())
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(TimeUtil.getCurrentLocalTime());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO, channelMemberInfo);
    }
}
