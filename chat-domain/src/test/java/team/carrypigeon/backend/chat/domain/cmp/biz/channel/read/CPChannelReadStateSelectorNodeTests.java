package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelReadStateSelectorNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_stateNotExists_shouldReturnDefault() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);

        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);

        node.process(null, context);

        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertNotNull(state);
        assertEquals(1L, state.getUid());
        assertEquals(2L, state.getCid());
        assertEquals(0L, state.getLastReadTime());
    }

    @Test
    void process_stateExists_shouldReturnExistingAndCache() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadState existing = new CPChannelReadState().setUid(1L).setCid(2L).setLastReadTime(10L);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(existing);

        CPChannelReadStateSelectorNode node = new CPChannelReadStateSelectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);

        node.process(null, context);
        node.process(null, context);

        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertSame(existing, state);
        verify(dao, times(1)).getByUidAndCid(1L, 2L);
    }
}

