package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
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
@LiteflowComponent("CPChannelBuilder")
public class CPChannelBuilderNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long channelInfoId = context.getData("ChannelInfo_Id");
        String channelInfoName = context.getData("ChannelInfo_Name");
        Long channelInfoOwner = context.getData("ChannelInfo_Owner");
        String channelInfoBrief = context.getData("ChannelInfo_Brief");
        Long channelInfoAvatar = context.getData("ChannelInfo_Avatar");
        Long channelInfoCreateTime = context.getData("ChannelInfo_CreateTime");
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
        context.setData("ChannelInfo",cpChannel);
    }
}
