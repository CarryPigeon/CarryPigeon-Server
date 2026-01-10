package team.carrypigeon.backend.chat.domain.cmp.basic;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CheckerResultReaderNodeTests {

    @Test
    void processSwitch_resultNull_shouldThrowArgsError() {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode(null);

        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void processSwitch_modeDefaultState_shouldReturnSuccessOrFail() throws Exception {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode(null);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
        assertEquals("success", node.process(null, context));

        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(false, null));
        assertEquals("fail", node.process(null, context));
    }

    @Test
    void processSwitch_modeMsg_shouldReturnMsgOrFallback() throws Exception {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode("msg");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, "tag"));
        assertEquals("tag", node.process(null, context));

        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(false, ""));
        assertEquals("fail", node.process(null, context));
    }

    @Test
    void processSwitch_invalidMode_shouldThrowArgsError() {
        TestableCheckerResultReaderNode node = new TestableCheckerResultReaderNode("unknown");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    private static final class TestableCheckerResultReaderNode extends CheckerResultReaderNode {
        private final String mode;

        private TestableCheckerResultReaderNode(String mode) {
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
