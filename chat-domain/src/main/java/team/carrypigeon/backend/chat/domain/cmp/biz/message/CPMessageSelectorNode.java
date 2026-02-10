package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

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
public class CPMessageSelectorNode extends AbstractSelectorNode<CPMessage> {

    private final ChannelMessageDao channelMessageDao;

    /**
     * 读取当前节点的执行模式。
     *
     * @param context LiteFlow 上下文（此节点固定使用 id 模式）
     * @return 固定模式字符串 { id}
     */
    @Override
    protected String readMode(CPFlowContext context) {
        return "id";
    }

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（当前固定为 { id}）
     * @param context LiteFlow 上下文（此节点固定使用 id 模式）
     * @return 命中的消息实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPMessage doSelect(String mode, CPFlowContext context) throws Exception {
        Long mid = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_ID);
        return select(context,
                buildSelectKey("message", "id", mid),
                () -> channelMessageDao.getById(mid));
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 消息实体写入键 { MESSAGE_INFO}
     */
    @Override
    protected CPKey<CPMessage> getResultKey() {
        return CPNodeMessageKeys.MESSAGE_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式
     * @param context LiteFlow 上下文（此节点固定使用 id 模式）
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        log.debug("CPMessageSelector: message not found");
        fail(CPProblem.of(CPProblemReason.NOT_FOUND, "message not found"));
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param mode 查询模式
     * @param entity 查询成功的消息实体
     * @param context LiteFlow 上下文（此节点固定使用 id 模式）
     */
    @Override
    protected void afterSuccess(String mode, CPMessage entity, CPFlowContext context) {
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, entity.getCid());
        log.debug("CPMessageSelector success, mid={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
    }
}
