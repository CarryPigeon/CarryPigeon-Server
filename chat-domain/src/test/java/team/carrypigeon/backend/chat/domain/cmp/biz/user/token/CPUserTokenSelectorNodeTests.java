package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
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
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, "t");

        node.process(null, context);
        assertSame(entity, context.get(CPNodeUserTokenKeys.USER_TOKEN_INFO));

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
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO_ID, 2L);

        node.process(null, context);
        assertSame(entity, context.get(CPNodeUserTokenKeys.USER_TOKEN_INFO));
        verify(dao).getById(2L);
    }

    @Test
    void process_invalidMode_shouldThrowArgsError() {
        UserTokenDao dao = mock(UserTokenDao.class);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("unknown");

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_notFound_shouldThrowBusinessError() {
        UserTokenDao dao = mock(UserTokenDao.class);
        when(dao.getByToken("t")).thenReturn(null);

        TestableCPUserTokenSelectorNode node = new TestableCPUserTokenSelectorNode(dao);
        node.setMode("token");

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, "t");

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(404, ex.getProblem().status());
        assertEquals("not_found", ex.getProblem().reason().code());
    }

    private static final class TestableCPUserTokenSelectorNode extends CPUserTokenSelectorNode {
        private String mode;

        /**
         * 构造测试辅助对象。
         *
         * @param userTokenDao 测试输入参数
         */
        private TestableCPUserTokenSelectorNode(UserTokenDao userTokenDao) {
            super(userTokenDao);
        }

        /**
         * 测试辅助方法。
         *
         * @param mode 测试输入参数
         */
        private void setMode(String mode) {
            this.mode = mode;
        }

        /**
         * 测试辅助方法。
         *
         * @param key 测试输入参数
         * @param clazz 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("key".equals(key) && clazz == String.class) {
                return clazz.cast(mode);
            }
            return null;
        }
    }
}
