package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPUserTokenSelectorNodeTests {

    @Test
    void process_modeToken_shouldSelectAndCache() throws Exception {
        UserTokenDao dao = mock(UserTokenDao.class);
        CPUserToken entity = new CPUserToken().setId(1L).setToken("t");
        when(dao.getByToken("t")).thenReturn(entity);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("token");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, "t");

        node.process(null, context);
        assertSame(entity, context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO));

        node.process(null, context);
        verify(dao, times(1)).getByToken("t");
    }

    @Test
    void process_modeId_shouldSelect() throws Exception {
        UserTokenDao dao = mock(UserTokenDao.class);
        CPUserToken entity = new CPUserToken().setId(2L);
        when(dao.getById(2L)).thenReturn(entity);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("id");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO_ID, 2L);

        node.process(null, context);
        assertSame(entity, context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO));
        verify(dao).getById(2L);
    }

    @Test
    void process_invalidMode_shouldThrowArgsError() {
        UserTokenDao dao = mock(UserTokenDao.class);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("unknown");

        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_notFound_shouldThrowBusinessError() {
        UserTokenDao dao = mock(UserTokenDao.class);
        when(dao.getByToken("t")).thenReturn(null);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("token");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, "t");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("token does not exists", response.getData().get("msg").asText());
    }

    private static final class TestableCPUserTokenSelectorNode extends CPUserTokenSelectorNode {
        private String mode;

        private TestableCPUserTokenSelectorNode(UserTokenDao userTokenDao) {
            super(userTokenDao);
        }

        private void setMode(String mode) {
            this.mode = mode;
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("key".equals(key) && clazz == String.class) {
                return clazz.cast(mode);
            }
            return null;
        }
    }
}

