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
 * 根据消息 id 查询消息的组件。<br/>
 * 入参：MessageInfo_Id:Long<br/>
 * 出参：
 *  - MessageInfo:{@link CPMessage}<br/>
 *  - ChannelInfo_Id:Long
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageSelector")
public class CPMessageSelectorNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long mid = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_ID);
        if (mid == null) {
            log.error("CPMessageSelector args error: MessageInfo_Id is null");
            argsError(context);
            return;
        }
        CPMessage message = channelMessageDao.getById(mid);
        if (message == null) {
            log.debug("CPMessageSelector: message not found, mid={}", mid);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("message not found"));
            throw new CPReturnException();
        }
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, message);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, message.getCid());
        log.debug("CPMessageSelector success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
