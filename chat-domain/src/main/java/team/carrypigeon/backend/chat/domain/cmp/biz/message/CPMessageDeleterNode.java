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
 * 删除消息的组件。<br/>
 * 入参：MessageInfo:{@link CPMessage}<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageDeleter")
public class CPMessageDeleterNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPMessage message = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO);
        if (message == null) {
            log.error("CPMessageDeleter args error: MessageInfo is null");
            argsError(context);
            return;
        }
        if (!channelMessageDao.delete(message)) {
            log.error("CPMessageDeleter delete failed, mid={}, cid={}, uid={}",
                    message.getId(), message.getCid(), message.getUid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("delete message error"));
            throw new CPReturnException();
        }
        log.info("CPMessageDeleter success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
