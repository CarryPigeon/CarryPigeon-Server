package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 频道申请保存Node<br/>
 * 入参：<br/>
 * ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参：无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationSavor")
public class CPChannelApplicationSavorNode extends CPNodeComponent {

    private final ChannelApplicationDAO channelApplicationDAO;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplication = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO);
        if (channelApplication==null){
            argsError(context);
        }
        if (!channelApplicationDAO.save(channelApplication)){
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.setTextData("save channel application error"));
            throw new CPReturnException();
        }
    }
}
