package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
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
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {

        Long channelInfoId = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID, Long.class);
        String channelInfoName = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_NAME, String.class);
        Long channelInfoOwner = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_OWNER, Long.class);
        String channelInfoBrief = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_BRIEF, String.class);
        Long channelInfoAvatar = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_AVATAR, Long.class);
        Long channelInfoCreateTime = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_CREATE_TIME, Long.class);
        CPChannel cpChannel = new CPChannel();
        cpChannel.setId(channelInfoId)
                .setName(channelInfoName)
                .setOwner(channelInfoOwner)
                .setBrief(channelInfoBrief)
                .setAvatar(channelInfoAvatar)
                .setCreateTime(TimeUtil.MillisToLocalDateTime(channelInfoCreateTime));
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, cpChannel);
    }
}
