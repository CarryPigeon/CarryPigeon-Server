package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
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

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPMessage message = context.getData(CPNodeMessageKeys.MESSAGE_INFO);
        if (message == null) {
            log.error("CPMessageDeleteNotifyBuilder args error: MessageInfo is null");
            argsError(context);
            return;
        }
        long sendTimeMillis = message.getSendTime() != null
                ? TimeUtil.LocalDateTimeToMillis(message.getSendTime())
                : TimeUtil.getCurrentTime();
        CPMessageNotificationData data = new CPMessageNotificationData()
                .setSContent("message deleted")
                .setCid(message.getCid())
                .setUid(message.getUid())
                .setSendTime(sendTimeMillis);
        context.setData(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPMessageDeleteNotifyBuilder success, mid={}, cid={}, uid={}",
                message.getId(), message.getCid(), message.getUid());
    }
}
