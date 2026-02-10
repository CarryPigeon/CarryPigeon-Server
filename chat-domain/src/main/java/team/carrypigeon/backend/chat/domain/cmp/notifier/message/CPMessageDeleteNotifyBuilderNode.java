package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.connection.notification.CPMessageNotificationData;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 消息删除通知数据构建组件。<br/>
 * 入参：MessageInfo:{@link CPMessage}<br/>
 * 出参：Notifier_Data:JsonNode
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageDeleteNotifyBuilder")
public class CPMessageDeleteNotifyBuilderNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取消息删除数据并构建通知 payload
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPMessage message = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO);
        long sendTimeMillis = message.getSendTime() != null
                ? TimeUtil.localDateTimeToMillis(message.getSendTime())
                : TimeUtil.currentTimeMillis();
        CPMessageNotificationData data = new CPMessageNotificationData()
                .setType("delete")
                .setSContent("message deleted")
                .setCid(message.getCid())
                .setUid(message.getUid())
                .setSendTime(sendTimeMillis);
        context.set(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPMessageDeleteNotifyBuilder success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
