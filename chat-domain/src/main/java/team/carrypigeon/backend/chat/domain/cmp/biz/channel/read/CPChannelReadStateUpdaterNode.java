package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * Upsert channel read state for current user in a channel.
 * <p>
 * Inputs:
 *  - session_uid:Long (current user id, written by UserLoginChecker)
 *  - ChannelReadStateInfo_Cid:Long
 *  - ChannelReadStateInfo_LastReadMid:Long (mid)
 *  - ChannelReadStateInfo_LastReadTime:Long (millis)
 * Output:
 *  - ChannelReadStateInfo:CPChannelReadState (updated entity)
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelReadStateUpdater")
public class CPChannelReadStateUpdaterNode extends CPNodeComponent {

    private final ChannelReadStateDao channelReadStateDao;
    private final ApiWsEventPublisher wsEventPublisher;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Long cid = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID);
        Long lastReadMid = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID);
        Long lastReadTimeMillis = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME);
        if (lastReadMid <= 0 || lastReadTimeMillis <= 0) {
            log.error("CPChannelReadStateUpdater args error, uid={}, cid={}, lastReadMid={}, lastReadTimeMillis={}",
                    uid, cid, lastReadMid, lastReadTimeMillis);
            validationFailed();
            return;
        }

        CPChannelReadState state = channelReadStateDao.getByUidAndCid(uid, cid);
        if (state == null) {
            state = new CPChannelReadState()
                    .setUid(uid)
                    .setCid(cid)
                    .setLastReadMid(0L)
                    .setLastReadTime(0L);
        }

        // Only move forward the read position.
        long oldMid = state.getLastReadMid();
        long oldTime = state.getLastReadTime();
        long newMid = lastReadMid;
        long newTime = lastReadTimeMillis;

        boolean moved = false;
        if (newMid > oldMid) {
            state.setLastReadMid(newMid);
            moved = true;
        }
        if (newTime > oldTime) {
            state.setLastReadTime(newTime);
            moved = true;
        }
        if (!moved) {
            log.debug("CPChannelReadStateUpdater ignored non-forward read position, uid={}, cid={}, oldMid={}, newMid={}, oldTime={}, newTime={}",
                    uid, cid, oldMid, newMid, oldTime, newTime);
        }

        boolean success = channelReadStateDao.save(state);
        if (!success) {
            log.error("CPChannelReadStateUpdater save failed, uid={}, cid={}", uid, cid);
            fail(CPProblem.of(500, "internal_error", "failed to save channel read state"));
        }

        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO, state);
        wsEventPublisher.publishReadStateUpdated(state);
        log.debug("CPChannelReadStateUpdater success, uid={}, cid={}, lastReadMid={}, lastReadTime={}",
                uid, cid, state.getLastReadMid(), state.getLastReadTime());
    }
}
