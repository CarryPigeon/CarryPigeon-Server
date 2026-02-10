package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelReadStateSelectorNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_stateNotExists_shouldReturnDefault() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);

        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);

        node.process(null, context);

        CPChannelReadState state = context.get(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertNotNull(state);
        assertEquals(1L, state.getUid());
        assertEquals(2L, state.getCid());
        assertEquals(0L, state.getLastReadMid());
        assertEquals(0L, state.getLastReadTime());
    }

    @Test
    void process_stateExists_shouldReturnExistingAndCache() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadState existing = new CPChannelReadState().setUid(1L).setCid(2L).setLastReadMid(9L).setLastReadTime(10L);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(existing);

        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);

        node.process(null, context);
        node.process(null, context);

        CPChannelReadState state = context.get(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertSame(existing, state);
        verify(dao, times(1)).getByUidAndCid(1L, 2L);
    }
}
