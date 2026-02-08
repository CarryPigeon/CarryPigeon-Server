package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * 删除消息的组件。<br/>
 * 入参：MessageInfo:{@link CPMessage}<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageDeleter")
public class CPMessageDeleterNode extends AbstractDeleteNode<CPMessage> {

    private final ChannelMessageDao channelMessageDao;
    private final ApiWsEventPublisher wsEventPublisher;

    @Override
    protected CPKey<CPMessage> getContextKey() {
        return CPNodeMessageKeys.MESSAGE_INFO;
    }

    @Override
    protected Class<CPMessage> getEntityClass() {
        return CPMessage.class;
    }

    @Override
    protected boolean doDelete(CPMessage message) {
        return channelMessageDao.delete(message);
    }

    @Override
    protected String getErrorMessage() {
        return "delete message error";
    }

    @Override
    protected void onFailure(CPMessage message, CPFlowContext context) {
        if (message != null) {
            log.error("CPMessageDeleter delete failed, mid={}, cid={}, uid={}",
                    message.getId(), message.getCid(), message.getUid());
        }
        fail(CPProblem.of(500, "internal_error", "delete message error"));
    }

    @Override
    protected void afterSuccess(CPMessage message, CPFlowContext context) {
        log.info("CPMessageDeleter success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
        wsEventPublisher.publishMessageDeleted(message.getCid(), message.getId());
    }
}
