package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.connection.notification.CPChannelReadStateNotificationData;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

/**
 * Build notification data for channel read state update.
 * <p>
 * Input:
 *  - ChannelReadStateInfo:{@link CPChannelReadState}
 * Output:
 *  - Notifier_Data:JsonNode
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelReadStateNotifyBuilder")
public class CPChannelReadStateNotifyBuilderNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取已读状态变化并构建通知 payload
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelReadState state = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        CPChannelReadStateNotificationData data = new CPChannelReadStateNotificationData()
                .setCid(state.getCid())
                .setUid(state.getUid())
                .setLastReadTime(state.getLastReadTime());
        context.set(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPChannelReadStateNotifyBuilder success, uid={}, cid={}, lastReadTime={}",
                state.getUid(), state.getCid(), state.getLastReadTime());
    }
}
