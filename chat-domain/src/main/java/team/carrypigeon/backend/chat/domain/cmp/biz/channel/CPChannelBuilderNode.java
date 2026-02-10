package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于构建通道信息的Node<br/>
 * 入参：<br/>
 * 1. ChannelInfo_Id:Long<br/>
 * 2. ChannelInfo_Name:String<br/>
 * 3. ChannelInfo_Owner:Long<br/>
 * 4. ChannelInfo_Brief:String<br/>
 * 5. ChannelInfo_Avatar:Long<br/>
 * 6. ChannelInfo_CreateTime:Long<br/>
 * 出参: ChannelInfo:{@link CPChannel}<br/>
 * */
@Slf4j
@LiteflowComponent("CPChannelBuilder")
public class CPChannelBuilderNode extends CPNodeComponent {
    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取频道字段并组装 { CPChannel}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {

        Long channelInfoId = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        String channelInfoName = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_NAME);
        Long channelInfoOwner = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_OWNER);
        String channelInfoBrief = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_BRIEF);
        Long channelInfoAvatar = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_AVATAR);
        Long channelInfoCreateTime = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_CREATE_TIME);
        CPChannel cpChannel = new CPChannel();
        cpChannel.setId(channelInfoId)
                .setName(channelInfoName)
                .setOwner(channelInfoOwner)
                .setBrief(channelInfoBrief)
                .setAvatar(channelInfoAvatar)
                .setCreateTime(TimeUtil.millisToLocalDateTime(channelInfoCreateTime));
        context.set(CPNodeChannelKeys.CHANNEL_INFO, cpChannel);
    }
}
