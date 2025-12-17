package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;

/**
 * 获取申请频道的id的Node<br/>
 * 入参: ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参: ChannelApplicationInfo_Cid:Long<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelApplicationCidGetter")
public class CPChannelApplicationCidGetterNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelApplication channelApplicationInfo =
                requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, CPChannelApplication.class);
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_CID, channelApplicationInfo.getCid());
    }
}
