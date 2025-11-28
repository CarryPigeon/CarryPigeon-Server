package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
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
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long startTime = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_UNREAD_START_TIME);
        Long uid = context.getData(CPNodeValueKeyBasicConstants.SESSION_ID);
        if (cid == null || uid == null || startTime == null) {
            log.error("CPMessageUnreadCounter args error, cid={}, uid={}, startTime={}",
                    cid, uid, startTime);
            argsError(context);
            return;
        }
        LocalDateTime start = TimeUtil.MillisToLocalDateTime(startTime);
        int count = channelMessageDao.getAfterCount(cid, uid, start);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_UNREAD_COUNT, (long) count);
        log.debug("CPMessageUnreadCounter success, cid={}, uid={}, startTime={}, count={}",
                cid, uid, startTime, count);
    }
}
