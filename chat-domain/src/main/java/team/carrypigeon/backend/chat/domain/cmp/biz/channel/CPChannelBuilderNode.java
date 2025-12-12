package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
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
    public void process(CPSession session, DefaultContext context) throws Exception {

        Long channelInfoId = context.getData(CPNodeChannelKeys.CHANNEL_INFO_ID);
        String channelInfoName = context.getData(CPNodeChannelKeys.CHANNEL_INFO_NAME);
        Long channelInfoOwner = context.getData(CPNodeChannelKeys.CHANNEL_INFO_OWNER);
        String channelInfoBrief = context.getData(CPNodeChannelKeys.CHANNEL_INFO_BRIEF);
        Long channelInfoAvatar = context.getData(CPNodeChannelKeys.CHANNEL_INFO_AVATAR);
        Long channelInfoCreateTime = context.getData(CPNodeChannelKeys.CHANNEL_INFO_CREATE_TIME);
        if (channelInfoId == null || channelInfoName == null || channelInfoOwner == null || channelInfoBrief == null || channelInfoAvatar == null || channelInfoCreateTime == null){
            argsError(context);
        }
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
