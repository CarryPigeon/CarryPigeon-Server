package team.carrypigeon.backend.chat.domain.cmp.notifier.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
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

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        if (state == null) {
            log.error("CPChannelReadStateNotifyBuilder args error: ChannelReadStateInfo is null");
            argsError(context);
            return;
        }
        CPChannelReadStateNotificationData data = new CPChannelReadStateNotificationData()
                .setCid(state.getCid())
                .setUid(state.getUid())
                .setLastReadTime(state.getLastReadTime());
        context.setData(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPChannelReadStateNotifyBuilder success, uid={}, cid={}, lastReadTime={}",
                state.getUid(), state.getCid(), state.getLastReadTime());
    }
}

