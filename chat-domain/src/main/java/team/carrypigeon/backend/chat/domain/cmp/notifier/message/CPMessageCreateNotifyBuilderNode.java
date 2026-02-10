package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.connection.notification.CPMessageNotificationData;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 消息创建通知数据构建组件。<br/>
 * 入参：
 *  - MessageInfo:{@link CPMessage}
 *  - MessageData:{@link CPMessageData}
 * 出参：
 *  - Notifier_Data:JsonNode
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageCreateNotifyBuilder")
public class CPMessageCreateNotifyBuilderNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取消息创建数据并构建通知 payload
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPMessage message = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO);
        CPMessageData messageData = requireContext(context, CPNodeValueKeyExtraConstants.MESSAGE_DATA);
        long sendTimeMillis = message.getSendTime() != null
                ? TimeUtil.localDateTimeToMillis(message.getSendTime())
                : TimeUtil.currentTimeMillis();
        CPMessageNotificationData data = new CPMessageNotificationData()
                .setType("create")
                .setSContent(messageData.getSContent())
                .setMid(message.getId())
                .setCid(message.getCid())
                .setUid(message.getUid())
                .setSendTime(sendTimeMillis);
        context.set(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPMessageCreateNotifyBuilder success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
