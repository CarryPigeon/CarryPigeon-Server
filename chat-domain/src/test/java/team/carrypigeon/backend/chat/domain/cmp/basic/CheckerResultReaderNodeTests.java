package team.carrypigeon.backend.chat.domain.cmp.basic;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CheckerResultReaderNodeTests {

    @Test
    void processSwitch_resultNull_shouldThrowArgsError() {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode(null);

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.run(context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void processSwitch_modeDefaultState_shouldReturnSuccessOrFail() throws Exception {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode(null);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, null));
        assertEquals("success", node.run(context));

        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(false, null));
        assertEquals("fail", node.run(context));
    }

    @Test
    void processSwitch_modeMsg_shouldReturnMsgOrFallback() throws Exception {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode("msg");

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, "tag"));
        assertEquals("tag", node.run(context));

        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(false, ""));
        assertEquals("fail", node.run(context));
    }

    @Test
    void processSwitch_invalidMode_shouldThrowArgsError() {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode("unknown");

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, null));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.run(context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    private static final class TestableCheckerResultReaderNode extends CheckerResultReaderNode {
        private final String mode;

        private TestableCheckerResultReaderNode(String mode) {
            this.mode = mode;
        }

        private String run(CPFlowContext context) throws Exception {
            return super.process(context);
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
