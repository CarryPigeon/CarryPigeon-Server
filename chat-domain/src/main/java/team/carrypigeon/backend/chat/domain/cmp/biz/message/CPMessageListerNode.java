package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

/**
 * 按时间倒序拉取频道历史消息列表。<br/>
 * 输入：<br/>
 *  - ChannelInfo_Id:Long  目标频道 id<br/>
 *  - MessageList_CursorMid:Long  拉取游标消息 id（mid，exclusive），空或非法时使用 MAX_VALUE<br/>
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
    protected void process(CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long cursorMid = context.get(CPNodeMessageKeys.MESSAGE_LIST_CURSOR_MID);
        Integer count = requireContext(context, CPNodeMessageKeys.MESSAGE_LIST_COUNT);
        if (cursorMid == null || cursorMid <= 0) {
            cursorMid = Long.MAX_VALUE;
        }
        Long finalCursor = cursorMid;
        CPMessage[] messages = select(context,
                buildSelectKey("message_list", java.util.Map.of("cid", cid, "count", count, "cursorMid", finalCursor)),
                () -> channelMessageDao.listBefore(cid, finalCursor, count));
        if (messages == null) {
            messages = new CPMessage[0];
        }
        context.set(CPNodeMessageKeys.MESSAGE_LIST, messages);
        log.debug("CPMessageLister success, cid={}, cursorMid={}, count={}, resultCount={}",
                cid, cursorMid, count, messages.length);
    }
}
