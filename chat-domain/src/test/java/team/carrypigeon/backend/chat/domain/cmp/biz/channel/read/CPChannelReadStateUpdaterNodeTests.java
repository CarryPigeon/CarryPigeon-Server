package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelReadStateUpdaterNodeTests {

    @Test
    void process_argsInvalid_shouldThrowArgsError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao, ws);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID, 10L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 0L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_stateNotExists_shouldCreateAndSave() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);
        when(dao.save(any())).thenReturn(true);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao, ws);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID, 10L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 100L);

        node.process(null, context);

        CPChannelReadState state = context.get(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertNotNull(state);
        assertEquals(1L, state.getUid());
        assertEquals(2L, state.getCid());
        assertEquals(10L, state.getLastReadMid());
        assertEquals(100L, state.getLastReadTime());
        verify(dao).save(state);
    }

    @Test
    void process_newTimeOlder_shouldNotMoveBack() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        CPChannelReadState existing = new CPChannelReadState().setUid(1L).setCid(2L).setLastReadMid(100L).setLastReadTime(1000L);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(existing);
        when(dao.save(any())).thenReturn(true);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao, ws);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID, 50L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 500L);

        node.process(null, context);

        CPChannelReadState state = context.get(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertSame(existing, state);
        assertEquals(100L, state.getLastReadMid());
        assertEquals(1000L, state.getLastReadTime());
    }

    @Test
    void process_saveFail_shouldThrowBusinessError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);
        when(dao.save(any())).thenReturn(false);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao, ws);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID, 10L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 100L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason().code());
    }
}
