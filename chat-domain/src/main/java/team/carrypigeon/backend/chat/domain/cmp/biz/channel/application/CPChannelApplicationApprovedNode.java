package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
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
    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param session 当前请求会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取申请记录并更新审批状态
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO);
        CPChannelMember channelMemberInfo = new CPChannelMember();
        channelMemberInfo
                .setId(IdUtil.generateId())
                .setCid(channelApplicationInfo.getCid())
                .setUid(channelApplicationInfo.getUid())
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(TimeUtil.currentLocalDateTime());
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, channelMemberInfo);
    }
}
