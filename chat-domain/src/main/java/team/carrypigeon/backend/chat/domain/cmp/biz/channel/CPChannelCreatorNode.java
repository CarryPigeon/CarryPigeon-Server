package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
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
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUser userInfo = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);
        if (userInfo == null){
            argsError(context);
        }
        CPChannel channelInfo = new CPChannel();
        channelInfo.setId(IdUtil.generateId())
                .setName(IdUtil.generateId()+"")
                .setOwner(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class))
                .setBrief("")
                .setCreateTime(TimeUtil.getCurrentLocalTime())
                .setAvatar(-1);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channelInfo);
    }
}
