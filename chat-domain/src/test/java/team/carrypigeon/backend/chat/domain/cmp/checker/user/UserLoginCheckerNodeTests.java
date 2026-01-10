package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginCheckerNodeTests {

    @Test
    void process_notLogin_hard_shouldThrowAuthorityError() {
        TestableUserLoginCheckerNode node = new TestableUserLoginCheckerNode(null);
        CPFlowContext context = new CPFlowContext();

        assertThrows(CPReturnException.class, () -> node.process(new TestSession(), context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(300, response.getCode());
        assertEquals("user not login", response.getData().get("msg").asText());
    }

    @Test
    void process_notLogin_soft_shouldWriteCheckResult() throws Exception {
        TestableUserLoginCheckerNode node = new TestableUserLoginCheckerNode("soft");
        CPFlowContext context = new CPFlowContext();

        node.process(new TestSession(), context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
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

        assertEquals(Long.valueOf(123L), context.getData(CPNodeCommonKeys.SESSION_ID));
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    private static final class TestableUserLoginCheckerNode extends UserLoginCheckerNode {
        private final String type;

        private TestableUserLoginCheckerNode(String type) {
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
