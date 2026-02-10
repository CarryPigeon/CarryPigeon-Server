package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPMessageDeleterNodeTests {

    @Test
    void process_deleteSuccess_shouldNotThrow() throws Exception {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(true);
        CPMessageDeleterNode node = new CPMessageDeleterNode(dao, ws);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO, msg);

        node.process(null, context);
        assertNull(context.get(CPFlowKeys.RESPONSE));
        verify(dao).delete(msg);
    }

    @Test
    void process_deleteFail_shouldThrowBusinessError() {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(false);
        CPMessageDeleterNode node = new CPMessageDeleterNode(dao, ws);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO, msg);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason().code());
    }
}
