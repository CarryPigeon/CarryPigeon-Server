package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

/**
 * 计算未读消息数量的组件。<br/>
 * 入参：
 *  - ChannelInfo_Id:Long
 *  - MessageUnread_StartTime:Long(millis)
 *  - SessionId:Long (由 UserLoginChecker 写入)
 * 出参：
 *  - MessageUnread_Count:Long
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageUnreadCounter")
public class CPMessageUnreadCounterNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID, Long.class);
        Long startTime = requireContext(context, CPNodeMessageKeys.MESSAGE_UNREAD_START_TIME, Long.class);
        Long uid = requireContext(context, CPNodeCommonKeys.SESSION_ID, Long.class);
        LocalDateTime start = TimeUtil.MillisToLocalDateTime(startTime);
        Integer countObj = select(context,
                buildSelectKey("message_unread_count", java.util.Map.of("cid", cid, "startTime", startTime, "uid", uid)),
                () -> channelMessageDao.getAfterCount(cid, uid, start));
        int count = countObj == null ? 0 : countObj;
        context.setData(CPNodeMessageKeys.MESSAGE_UNREAD_COUNT, (long) count);
        log.debug("CPMessageUnreadCounter success, cid={}, uid={}, startTime={}, count={}",
                cid, uid, startTime, count);
    }
}
