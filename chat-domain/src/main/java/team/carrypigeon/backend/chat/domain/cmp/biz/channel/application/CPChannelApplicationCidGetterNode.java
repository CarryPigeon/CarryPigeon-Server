package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 获取申请频道的id的Node<br/>
 * 入参: ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参: ChannelApplicationInfo_Cid:Long<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelApplicationCidGetter")
public class CPChannelApplicationCidGetterNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO);
        if (channelApplicationInfo==null){
            argsError(context);
            return;
        }
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO_CID, channelApplicationInfo.getCid());
    }
}
