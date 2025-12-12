package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 将消息持久化到数据库的节点。<br/>
 * 输入：MessageInfo:{@link CPMessage}<br/>
 * 输出：无；保存失败时抛出 {@link CPReturnException} 并设置错误响应。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageSaver")
public class CPMessageSaverNode extends AbstractSaveNode<CPMessage> {

    private final ChannelMessageDao channelMessageDao;

    @Override
    protected String getContextKey() {
        return CPNodeMessageKeys.MESSAGE_INFO;
    }

    @Override
    protected Class<CPMessage> getEntityClass() {
        return CPMessage.class;
    }

    @Override
    protected boolean doSave(CPMessage entity) {
        return channelMessageDao.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save message error";
    }

    @Override
    protected void afterSuccess(CPMessage entity, com.yomahub.liteflow.slot.DefaultContext context) {
        log.debug("CPMessageSaver success, mid={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
    }

    @Override
    protected void onFailure(CPMessage entity, com.yomahub.liteflow.slot.DefaultContext context) {
        if (entity != null) {
            log.error("CPMessageSaver failed to save message, mid={}, cid={}, uid={}",
                    entity.getId(), entity.getCid(), entity.getUid());
        } else {
            log.error("CPMessageSaver failed to save message: entity is null");
        }
    }
}
