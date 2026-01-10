package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPMessageSaverNodeTests {

    @Test
    void process_saveSuccess_shouldNotThrow() throws Exception {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.save(any())).thenReturn(true);
        CPMessageSaverNode node = new CPMessageSaverNode(dao);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, msg);

        node.process(null, context);
        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
        verify(dao).save(msg);
    }

    @Test
    void process_saveFail_shouldThrowBusinessError() {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.save(any())).thenReturn(false);
        CPMessageSaverNode node = new CPMessageSaverNode(dao);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, msg);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("save message error", response.getData().get("msg").asText());
    }

    @Test
    void onFailure_entityNull_shouldNotThrow() throws Exception {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ExposedCPMessageSaverNode node = new ExposedCPMessageSaverNode(dao);

        CPFlowContext context = new CPFlowContext();
        assertDoesNotThrow(() -> node.callOnFailure(null, context));
    }

    private static final class ExposedCPMessageSaverNode extends CPMessageSaverNode {
        private ExposedCPMessageSaverNode(ChannelMessageDao channelMessageDao) {
            super(channelMessageDao);
        }

        private void callOnFailure(CPMessage entity, CPFlowContext context) throws Exception {
            super.onFailure(entity, context);
        }
    }
}

