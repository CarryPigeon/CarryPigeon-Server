package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.chat.domain.cmp.base.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 申请保存token<br/>
 * 入参：annelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参：无br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationSaver")
public class CPChannelApplicationSaverNode extends CPNodeComponent {

    private final ChannelApplicationDAO channelApplicationDAO;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplication =
                context.getData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO);
        if (channelApplication == null) {
            argsError(context);
        }
        if (!channelApplicationDAO.save(channelApplication)) {
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.setTextData("save channel application error"));
            throw new CPReturnException();
        }
    }
}

