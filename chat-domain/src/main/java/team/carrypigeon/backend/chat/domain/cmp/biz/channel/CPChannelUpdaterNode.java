package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 用于更新通道的Node<br/>
 * 入参:<br/>
 * 1.ChannelInfo:{@link CPChannel}<br/>
 * 2.ChannelInfo_Name:String?<br/>
 * 3.ChannelInfo_Owner:Long?<br/>
 * 4.ChannelInfo_Brief:String?<br/>
 * 5.ChannelInfo_Avatar:Long?<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelUpdater")
public class CPChannelUpdaterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channelInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        if (channelInfo == null){
            argsError(context);
        }
        String channelInfoName = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_NAME);
        if (channelInfoName != null){
            channelInfo.setName(channelInfoName);
        }
        Long channelInfoOwner = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_OWNER);
        if (channelInfoOwner != null){
            channelInfo.setOwner(channelInfoOwner);
        }
        String channelInfoBrief = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_BRIEF);
        if (channelInfoBrief != null){
            channelInfo.setBrief(channelInfoBrief);
        }
        Long channelInfoAvatar = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_AVATAR);
        if (channelInfoAvatar != null){
            channelInfo.setAvatar(channelInfoAvatar);
        }
    }
}
