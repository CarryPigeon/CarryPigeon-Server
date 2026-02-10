package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailExistsCheckerTests {

    @Test
    void process_emailExists_hard_shouldThrowBusinessError() {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, null);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(409, ex.getProblem().status());
        assertEquals("email_exists", ex.getProblem().reason().code());
    }

    @Test
    void process_emailExists_soft_shouldWriteCheckResult() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, "soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("email exists", result.msg());
    }

    @Test
    void process_emailNotExists_soft_shouldWriteSuccess() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(null);

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, "soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        node.process(null, context);

        verify(userDao, times(2)).getByEmail("a@b.com");
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    @Test
    void process_emailExists_soft_shouldUseCacheForNonNullResult() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, "soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        node.process(null, context);

        verify(userDao, times(1)).getByEmail("a@b.com");
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
    }

    private static final class TestableEmailExistsChecker extends EmailExistsChecker {
        private final String type;

        /**
         * 构造测试辅助对象。
         *
         * @param userDao 测试输入参数
         * @param type 测试输入参数
         */
        private TestableEmailExistsChecker(UserDao userDao, String type) {
            super(userDao);
            this.type = type;
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
            if ("type".equals(key) && clazz == String.class) {
                return clazz.cast(type);
            }
            return null;
        }
    }
}
