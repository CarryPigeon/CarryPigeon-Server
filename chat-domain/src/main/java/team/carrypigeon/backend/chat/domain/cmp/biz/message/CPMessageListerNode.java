package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

/**
 * 按时间倒序拉取频道历史消息列表。<br/>
 * 输入：<br/>
 *  - ChannelInfo_Id:Long  目标频道 id<br/>
 *  - MessageList_StartTime:Long(millis)  起始时间，空或非法时使用当前时间<br/>
 *  - MessageList_Count:Integer  拉取条数上限<br/>
 * 输出：<br/>
 *  - Messages:CPMessage[]  查询到的消息数组，未命中时为空数组
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageLister")
public class CPMessageListerNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long startTime = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_LIST_START_TIME);
        Integer count = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_LIST_COUNT);
        if (cid == null || count == null) {
            log.error("CPMessageLister args error, cid={}, count={}", cid, count);
            argsError(context);
            return;
        }
        if (startTime == null || startTime <= 0) {
            startTime = TimeUtil.getCurrentTime();
        }
        LocalDateTime start = TimeUtil.MillisToLocalDateTime(startTime);
        CPMessage[] messages = channelMessageDao.getBefore(cid, start, count);
        if (messages == null) {
            messages = new CPMessage[0];
        }
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_LIST, messages);
        log.debug("CPMessageLister success, cid={}, startTime={}, count={}, resultCount={}",
                cid, startTime, count, messages.length);
    }
}
