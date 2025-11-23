package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 用于检查通道的创建者权限的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel};UserInfo_Id:Long<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPChannelOwnerChecker")
public class CPChannelOwnerChecker extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channelInfo = context.getData("ChannelInfo");
        Long userInfoId = context.getData("UserInfo_Id");
        if (channelInfo == null|| userInfoId == null){
            argsError(context);
        }
        if (channelInfo.getOwner() != userInfoId){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel"));
            throw new CPReturnException();
        }
    }
}
