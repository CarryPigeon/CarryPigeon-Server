package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidCheckerTests {

    @Test
    void process_invalidEmail_hard_shouldThrowBusinessError() {
        TestableEmailValidChecker node = new TestableEmailValidChecker(null);
        CPFlowContext context = new CPFlowContext();
        context.setData("Email", "bad");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("email error", response.getData().get("msg").asText());
    }

    @Test
    void process_invalidEmail_soft_shouldWriteCheckResult() throws Exception {
        TestableEmailValidChecker node = new TestableEmailValidChecker("soft");
        CPFlowContext context = new CPFlowContext();
        context.setData("Email", "bad");

        node.process(null, context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("email error", result.msg());
    }

    @Test
    void process_validEmail_soft_shouldWriteSuccess() throws Exception {
        TestableEmailValidChecker node = new TestableEmailValidChecker("soft");
        CPFlowContext context = new CPFlowContext();
        context.setData("Email", "a@b.com");

        node.process(null, context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    private static final class TestableEmailValidChecker extends EmailValidChecker {
        private final String type;

        private TestableEmailValidChecker(String type) {
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

