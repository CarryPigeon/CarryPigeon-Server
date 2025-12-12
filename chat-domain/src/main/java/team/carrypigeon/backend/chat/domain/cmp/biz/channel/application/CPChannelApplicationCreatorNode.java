package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 创建频道申请参数构建node<br/>
 * 入参：SessionId:Long;ChannelInfo:{@link CPChannel};ChannelMemberInfo_Msg:String<br/>
 * 出参：ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationCreator")
public class CPChannelApplicationCreatorNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        // 获取用户id与上下文参数
        Long uid = requireContext(context, CPNodeCommonKeys.SESSION_ID, Long.class);
        CPChannel cpChannel = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO, CPChannel.class);
        String msg = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_MSG, String.class);
        // 判断是否为固有频道
        if (cpChannel.getOwner()==-1){
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("channel is fixed"));
            throw new CPReturnException();
        }
        // 判断用户是否已加入该频道
        if (channelMemberDao.getMember(uid,cpChannel.getId())!=null){
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("you are already in this channel"));
            throw new CPReturnException();
        }
        CPChannelApplication application = new CPChannelApplication()
                .setId(IdUtil.generateId())
                .setCid(cpChannel.getId())
                .setUid(uid)
                .setState(CPChannelApplicationStateEnum.PENDING)
                .setApplyTime(TimeUtil.getCurrentLocalTime())
                .setMsg(msg);
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, application);
    }
}
