package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
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

    @Override
    protected String readMode(CPFlowContext context) {
        // 不依赖 bind，固定按 id 查询
        return "id";
    }

    @Override
    protected CPMessage doSelect(String mode, CPFlowContext context) throws Exception {
        Long mid = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_ID);
        return select(context,
                buildSelectKey("message", "id", mid),
                () -> channelMessageDao.getById(mid));
    }

    @Override
    protected CPKey<CPMessage> getResultKey() {
        return CPNodeMessageKeys.MESSAGE_INFO;
    }

    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        log.debug("CPMessageSelector: message not found");
        fail(CPProblem.of(404, "not_found", "message not found"));
    }

    @Override
    protected void afterSuccess(String mode, CPMessage entity, CPFlowContext context) {
        // 写入关联的频道 id，并打印日志
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, entity.getCid());
        log.debug("CPMessageSelector success, mid={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
    }
}
