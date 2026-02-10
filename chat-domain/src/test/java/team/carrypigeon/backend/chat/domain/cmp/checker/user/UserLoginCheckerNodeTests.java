package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginCheckerNodeTests {

    @Test
    void process_notLogin_hard_shouldThrowAuthorityError() {
        TestableUserLoginCheckerNode node = new TestableUserLoginCheckerNode(null);
        CPFlowContext context = new CPFlowContext();

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(new TestSession(), context));
        assertEquals(401, ex.getProblem().status());
        assertEquals("unauthorized", ex.getProblem().reason().code());
    }

    @Test
    void process_notLogin_soft_shouldWriteCheckResult() throws Exception {
        TestableUserLoginCheckerNode node = new TestableUserLoginCheckerNode("soft");
        CPFlowContext context = new CPFlowContext();

        node.process(new TestSession(), context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("user not login", result.msg());
    }

    @Test
    void process_login_soft_shouldWriteSessionIdAndCheckResult() throws Exception {
        TestableUserLoginCheckerNode node = new TestableUserLoginCheckerNode("soft");
        CPFlowContext context = new CPFlowContext();
        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, 123L);

        node.process(session, context);

        assertEquals(Long.valueOf(123L), context.get(CPFlowKeys.SESSION_UID));
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    private static final class TestableUserLoginCheckerNode extends UserLoginCheckerNode {
        private final String type;

        /**
         * 构造测试辅助对象。
         *
         * @param type 测试输入参数
         */
        private TestableUserLoginCheckerNode(String type) {
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
