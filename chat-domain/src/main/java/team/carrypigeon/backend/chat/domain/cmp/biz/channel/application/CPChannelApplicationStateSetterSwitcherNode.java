package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeSwitchComponent;

/**
 * 频道申请状态设置Node<br/>
 * 入参：<br/>
 * 1.ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 2.ChannelApplicationInfo_State:Integer<br/>
 * 出参：无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("ChannelApplicationStateSetterSwitcher")
public class CPChannelApplicationStateSetterSwitcherNode extends CPNodeSwitchComponent {
    @Override
    protected String process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = context.getData("ChannelApplicationInfo");
        Integer state = context.getData("ChannelApplicationInfo_State");
        if (channelApplicationInfo==null|| state==null){
            argsError(context);
        }
        assert state != null;
        int stateInt = state;
        if(state!=1&&state!=2){
            argsError(context);
        }
        assert channelApplicationInfo != null;
        channelApplicationInfo.setState(CPChannelApplicationStateEnum.valueOf(state));
        return switch (stateInt){
            case 1 -> "tag:approved";
            case 2 -> "tag:rejected";
            default -> "";
        };
    }
}
