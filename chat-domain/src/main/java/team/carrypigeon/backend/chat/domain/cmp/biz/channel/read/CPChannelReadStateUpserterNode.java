package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * Upsert channel read state for current user in a channel.
 * <p>
 * Inputs:
 *  - SessionId:Long (current user id, written by UserLoginChecker)
 *  - ChannelReadStateInfo_Cid:Long
 *  - ChannelReadStateInfo_LastReadTime:Long (millis)
 * Output:
 *  - ChannelReadStateInfo:CPChannelReadState (updated entity)
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelReadStateUpserter")
public class CPChannelReadStateUpserterNode extends CPNodeComponent {

    private final ChannelReadStateDao channelReadStateDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long uid = context.getData(CPNodeCommonKeys.SESSION_ID);
        Long cid = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID);
        Long lastReadTimeMillis = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME);

        if (uid == null || cid == null || lastReadTimeMillis == null || lastReadTimeMillis <= 0) {
            log.error("CPChannelReadStateUpserter args error, uid={}, cid={}, lastReadTimeMillis={}",
                    uid, cid, lastReadTimeMillis);
            argsError(context);
            return;
        }

        CPChannelReadState state = channelReadStateDao.getByUidAndCid(uid, cid);
        if (state == null) {
            state = new CPChannelReadState()
                    .setUid(uid)
                    .setCid(cid)
                    .setLastReadTime(0L);
        }

        // Only move forward the read position.
        long oldTime = state.getLastReadTime();
        long newTime = lastReadTimeMillis;
        if (newTime > oldTime) {
            state.setLastReadTime(newTime);
        } else {
            log.debug("CPChannelReadStateUpserter ignored older read time, uid={}, cid={}, old={}, new={}",
                    uid, cid, oldTime, newTime);
        }

        boolean success = channelReadStateDao.save(state);
        if (!success) {
            log.error("CPChannelReadStateUpserter save failed, uid={}, cid={}", uid, cid);
            businessError(context, "failed to save channel read state");
            return;
        }

        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO, state);
        log.debug("CPChannelReadStateUpserter success, uid={}, cid={}, lastReadTime={}",
                uid, cid, state.getLastReadTime());
    }
}
