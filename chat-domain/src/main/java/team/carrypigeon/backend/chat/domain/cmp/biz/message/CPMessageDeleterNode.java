package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 消息实体在上下文中的键 { MESSAGE_INFO}
     */
    @Override
    protected CPKey<CPMessage> getContextKey() {
        return CPNodeMessageKeys.MESSAGE_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPMessage}
     */
    @Override
    protected Class<CPMessage> getEntityClass() {
        return CPMessage.class;
    }

    /**
     * 执行删除操作并返回是否成功。
     *
     * @param message 待删除的消息实体
     * @return {@code true} 表示消息删除成功
     */
    @Override
    protected boolean doDelete(CPMessage message) {
        return channelMessageDao.delete(message);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 删除失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "delete message error";
    }

    /**
     * 处理失败后的补偿逻辑。
     *
     * @param message 删除失败的消息实体（可能为 {@code null}）
     * @param context LiteFlow 上下文，用于统一错误中断
     */
    @Override
    protected void onFailure(CPMessage message, CPFlowContext context) {
        if (message != null) {
            log.error("CPMessageDeleter delete failed, mid={}, cid={}, uid={}",
                    message.getId(), message.getCid(), message.getUid());
        }
        fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "delete message error"));
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param message 已删除成功的消息实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPMessage message, CPFlowContext context) {
        log.info("CPMessageDeleter success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
        wsEventPublisher.publishMessageDeleted(message.getCid(), message.getId());
    }
}
