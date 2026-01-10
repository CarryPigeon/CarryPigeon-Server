package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailExistsCheckerTests {

    @Test
    void process_emailExists_hard_shouldThrowBusinessError() {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, null);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("email exists", response.getData().get("msg").asText());
    }

    @Test
    void process_emailExists_soft_shouldWriteCheckResult() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, "soft");
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
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
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        node.process(null, context);

        verify(userDao, times(2)).getByEmail("a@b.com");
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    @Test
    void process_emailExists_soft_shouldUseCacheForNonNullResult() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.getByEmail("a@b.com")).thenReturn(new CPUser().setId(1L));

        TestableEmailExistsChecker node = new TestableEmailExistsChecker(userDao, "soft");
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);
        node.process(null, context);

        verify(userDao, times(1)).getByEmail("a@b.com");
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
    }

    private static final class TestableEmailExistsChecker extends EmailExistsChecker {
        private final String type;

        private TestableEmailExistsChecker(UserDao userDao, String type) {
            super(userDao);
            this.type = type;
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("type".equals(key) && clazz == String.class) {
                return clazz.cast(type);
            }
            return null;
        }
    }
}
