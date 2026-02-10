package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于创建通道的Node<br/>
 * 入参: UserInfo:{@link CPUser}<br/>
 * 出参: ChannelInfo:{@link CPChannel}<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelCreator")
public class CPChannelCreatorNode extends CPNodeComponent {
    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，基于当前用户创建默认频道实体
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPUser user = requireContext(context, CPNodeUserKeys.USER_INFO);
        CPChannel channelInfo = new CPChannel();
        channelInfo.setId(IdUtil.generateId())
                .setName(IdUtil.generateId()+"")
                .setOwner(user.getId())
                .setBrief("")
                .setCreateTime(TimeUtil.currentLocalDateTime())
                .setAvatar(-1);
        context.set(CPNodeChannelKeys.CHANNEL_INFO, channelInfo);
    }
}
