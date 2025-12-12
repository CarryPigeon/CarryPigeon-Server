package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
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

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPMessage message = context.getData(CPNodeMessageKeys.MESSAGE_INFO);
        CPMessageData messageData = context.getData(CPNodeValueKeyExtraConstants.MESSAGE_DATA);
        if (message == null || messageData == null) {
            log.error("CPMessageCreateNotifyBuilder args error: message or messageData is null");
            argsError(context);
            return;
        }
        long sendTimeMillis = message.getSendTime() != null
                ? TimeUtil.LocalDateTimeToMillis(message.getSendTime())
                : TimeUtil.getCurrentTime();
        CPMessageNotificationData data = new CPMessageNotificationData()
                .setSContent(messageData.getSContent())
                .setCid(message.getCid())
                .setUid(message.getUid())
                .setSendTime(sendTimeMillis);
        context.setData(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPMessageCreateNotifyBuilder success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
