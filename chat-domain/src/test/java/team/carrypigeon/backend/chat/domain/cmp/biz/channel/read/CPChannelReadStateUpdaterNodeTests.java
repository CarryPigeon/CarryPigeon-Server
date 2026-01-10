package team.carrypigeon.backend.chat.domain.cmp.biz.channel.read;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelReadStateUpdaterNodeTests {

    @Test
    void process_argsInvalid_shouldThrowArgsError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 0L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void process_stateNotExists_shouldCreateAndSave() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);
        when(dao.save(any())).thenReturn(true);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 100L);

        node.process(null, context);

        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertNotNull(state);
        assertEquals(1L, state.getUid());
        assertEquals(2L, state.getCid());
        assertEquals(100L, state.getLastReadTime());
        verify(dao).save(state);
    }

    @Test
    void process_newTimeOlder_shouldNotMoveBack() throws Exception {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        CPChannelReadState existing = new CPChannelReadState().setUid(1L).setCid(2L).setLastReadTime(1000L);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(existing);
        when(dao.save(any())).thenReturn(true);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 500L);

        node.process(null, context);

        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        assertSame(existing, state);
        assertEquals(1000L, state.getLastReadTime());
    }

    @Test
    void process_saveFail_shouldThrowBusinessError() {
        ChannelReadStateDao dao = mock(ChannelReadStateDao.class);
        when(dao.getByUidAndCid(1L, 2L)).thenReturn(null);
        when(dao.save(any())).thenReturn(false);

        CPChannelReadStateUpdaterNode node = new CPChannelReadStateUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, 2L);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, 100L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("failed to save channel read state", response.getData().get("msg").asText());
    }
}

