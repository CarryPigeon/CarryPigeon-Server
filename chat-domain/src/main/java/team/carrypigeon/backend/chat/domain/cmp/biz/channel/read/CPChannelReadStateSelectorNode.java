package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

/**
 * Selector node for channel read state of current user.
 * <p>
 * Inputs:
 *  - session_uid:Long
 *  - ChannelReadStateInfo_Cid:Long
 * Output:
 *  - ChannelReadStateInfo:CPChannelReadState
 * <p>
 * If no record exists in database, a transient state with lastReadTime=0 is returned.
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelReadStateSelector")
public class CPChannelReadStateSelectorNode extends CPNodeComponent {

    private final ChannelReadStateDao channelReadStateDao;

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取用户与频道并查询已读状态
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Long cid = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID);
        CPChannelReadState state = select(context,
                buildSelectKey("channel_read_state", java.util.Map.of("cid", cid, "uid", uid)),
                () -> channelReadStateDao.getByUidAndCid(uid, cid));
        if (state == null) {
            state = new CPChannelReadState()
                    .setUid(uid)
                    .setCid(cid)
                    .setLastReadMid(0L)
                    .setLastReadTime(0L);
            log.debug("CPChannelReadStateSelector no existing state, return default, uid={}, cid={}", uid, cid);
        } else {
            log.debug("CPChannelReadStateSelector found state, uid={}, cid={}, lastReadTime={}",
                    state.getUid(), state.getCid(), state.getLastReadTime());
        }
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO, state);
    }
}
