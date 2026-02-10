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

class CPMessageSaverNodeTests {

    @Test
    void process_saveSuccess_shouldNotThrow() throws Exception {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.save(any())).thenReturn(true);
        CPMessageSaverNode node = new CPMessageSaverNode(dao, ws);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO, msg);

        node.process(null, context);
        assertNull(context.get(CPFlowKeys.RESPONSE));
        verify(dao).save(msg);
    }

    @Test
    void process_saveFail_shouldThrowBusinessError() {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.save(any())).thenReturn(false);
        CPMessageSaverNode node = new CPMessageSaverNode(dao, ws);

        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO, msg);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason().code());
    }

    @Test
    void onFailure_entityNull_shouldNotThrow() throws Exception {
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        ExposedCPMessageSaverNode node = new ExposedCPMessageSaverNode(dao, ws);

        CPFlowContext context = new CPFlowContext();
        assertDoesNotThrow(() -> node.callOnFailure(null, context));
    }

    private static final class ExposedCPMessageSaverNode extends CPMessageSaverNode {
        /**
         * 构造测试辅助对象。
         *
         * @param channelMessageDao 测试输入参数
         * @param wsEventPublisher 测试输入参数
         */
        private ExposedCPMessageSaverNode(ChannelMessageDao channelMessageDao, ApiWsEventPublisher wsEventPublisher) {
            super(channelMessageDao, wsEventPublisher);
        }

        /**
         * 测试辅助方法。
         *
         * @param entity 测试输入参数
         * @param context 测试输入参数
         * @throws Exception 执行过程中抛出的异常
         */
        private void callOnFailure(CPMessage entity, CPFlowContext context) throws Exception {
            super.onFailure(entity, context);
        }
    }
}
