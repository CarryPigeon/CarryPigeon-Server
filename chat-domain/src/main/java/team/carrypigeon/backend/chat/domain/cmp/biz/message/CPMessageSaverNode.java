package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * 将消息持久化到数据库的节点。<br/>
 * 输入：MessageInfo:{@link CPMessage}<br/>
 * 输出：无；保存失败时抛出 {@link team.carrypigeon.backend.api.chat.domain.error.CPProblemException}。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageSaver")
public class CPMessageSaverNode extends AbstractSaveNode<CPMessage> {

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
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的消息实体
     * @return {@code true} 表示消息写库成功
     */
    @Override
    protected boolean doSave(CPMessage entity) {
        return channelMessageDao.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save message error";
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param entity 已保存成功的消息实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPMessage entity, CPFlowContext context) {
        log.debug("CPMessageSaver success, mid={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
        wsEventPublisher.publishMessageCreated(entity);
    }

    /**
     * 处理失败后的补偿逻辑。
     *
     * @param entity 保存失败的消息实体（可能为 {@code null}）
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void onFailure(CPMessage entity, CPFlowContext context) {
        if (entity != null) {
            log.error("CPMessageSaver failed to save message, mid={}, cid={}, uid={}",
                    entity.getId(), entity.getCid(), entity.getUid());
        } else {
            log.error("CPMessageSaver failed to save message: entity is null");
        }
    }
}
