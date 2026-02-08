package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 创建频道申请参数构建node<br/>
 * 入参：session_uid:Long;ChannelInfo:{@link CPChannel};ChannelMemberInfo_Msg:String<br/>
 * 出参：ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationCreator")
public class CPChannelApplicationCreatorNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        // 获取用户id与上下文参数
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        CPChannel cpChannel = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        String msg = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_MSG);
        // 判断是否为固有频道
        if (cpChannel.getOwner() == -1) {
            fail(CPProblem.of(422, "channel_fixed", "channel is fixed"));
        }
        // 判断用户是否已加入该频道
        long cid = cpChannel.getId();
        if (select(context,
                buildSelectKey("channel_member", java.util.Map.of("cid", cid, "uid", uid)),
                () -> channelMemberDao.getMember(uid, cid)) != null) {
            fail(CPProblem.of(409, "already_in_channel", "you are already in this channel"));
        }
        CPChannelApplication application = new CPChannelApplication()
                .setId(IdUtil.generateId())
                .setCid(cid)
                .setUid(uid)
                .setState(CPChannelApplicationStateEnum.PENDING)
                .setApplyTime(TimeUtil.currentLocalDateTime())
                .setMsg(msg);
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, application);
    }
}
