package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

/**
 * 频道申请状态设置Node<br/>
 * bind:String(accept|reject)
 * 入参：<br/>
 * ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参：无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("ChannelApplicationStateSetter")
public class CPChannelApplicationStateSetterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = context.getData("ChannelApplicationInfo");
        String bindData = getBindData("key", String.class);
        if (channelApplicationInfo==null|| bindData==null){
            argsError(context);
            return;
        }
        if (bindData.equals("accept")){
            channelApplicationInfo.setState(CPChannelApplicationStateEnum.APPROVED);
        }else if (bindData.equals("reject")){
            channelApplicationInfo.setState(CPChannelApplicationStateEnum.REJECTED);
        }else {
            argsError(context);
        }
    }
}
