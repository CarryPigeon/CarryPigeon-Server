package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * ????????<br/>
 * ???MessageInfo:{@link CPMessage}<br/>
 * ????
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageSaver")
public class CPMessageSaverNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPMessage message = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO);
        if (message == null) {
            log.error("CPMessageSaver args error: MessageInfo is null");
            argsError(context);
            return;
        }
        if (!channelMessageDao.save(message)) {
            log.error("CPMessageSaver failed to save message, mid={}, cid={}, uid={}",
                    message.getId(), message.getCid(), message.getUid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("save message error"));
            throw new CPReturnException();
        }
        log.debug("CPMessageSaver success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
