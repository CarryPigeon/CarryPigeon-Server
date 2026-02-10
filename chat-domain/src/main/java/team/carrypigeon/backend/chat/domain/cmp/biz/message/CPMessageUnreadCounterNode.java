package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

/**
 * 计算未读消息数量的组件。<br/>
 * 入参：
 *  - ChannelInfo_Id:Long
 *  - MessageUnread_StartMid:Long
 * 出参：
 *  - MessageUnread_Count:Long
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageUnreadCounter")
public class CPMessageUnreadCounterNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取频道游标并统计未读消息数量
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long startMid = requireContext(context, CPNodeMessageKeys.MESSAGE_UNREAD_START_MID);
        Integer countObj = select(context,
                buildSelectKey("message_unread_count", java.util.Map.of("cid", cid, "startMid", startMid)),
                () -> channelMessageDao.countAfter(cid, startMid));
        int count = countObj == null ? 0 : countObj;
        context.set(CPNodeMessageKeys.MESSAGE_UNREAD_COUNT, (long) count);
        log.debug("CPMessageUnreadCounter success, cid={}, startMid={}, count={}",
                cid, startMid, count);
    }
}
