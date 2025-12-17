package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * Selector node for channel read state of current user.
 * <p>
 * Inputs:
 *  - SessionId:Long
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

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long uid = context.getData(CPNodeCommonKeys.SESSION_ID);
        Long cid = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID);
        if (uid == null || cid == null) {
            log.error("CPChannelReadStateSelector args error, uid={}, cid={}", uid, cid);
            argsError(context);
            return;
        }
        CPChannelReadState state = select(context,
                buildSelectKey("channel_read_state", java.util.Map.of("cid", cid, "uid", uid)),
                () -> channelReadStateDao.getByUidAndCid(uid, cid));
        if (state == null) {
            state = new CPChannelReadState()
                    .setUid(uid)
                    .setCid(cid)
                    .setLastReadTime(0L);
            log.debug("CPChannelReadStateSelector no existing state, return default, uid={}, cid={}", uid, cid);
        } else {
            log.debug("CPChannelReadStateSelector found state, uid={}, cid={}, lastReadTime={}",
                    state.getUid(), state.getCid(), state.getLastReadTime());
        }
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO, state);
    }
}
