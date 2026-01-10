package team.carrypigeon.backend.api.chat.domain.node;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CPNodeComponentTests {

    @Test
    void buildSelectKey_single_shouldFormatAndHandleNulls() {
        TestableNode node = new TestableNode();
        assertEquals("t-f:f=1", node.callBuildSelectKey("t", "f", 1));
        assertEquals("-:=" + "null", node.callBuildSelectKey(null, null, null));
    }

    @Test
    void buildSelectKey_map_shouldSortKeysAndHandleEmpty() {
        TestableNode node = new TestableNode();
        assertEquals("t-:", node.callBuildSelectKey("t", Map.of()));
        assertEquals("t-a_b:a=1;b=2", node.callBuildSelectKey("t", Map.of("b", 2, "a", 1)));
    }

    @Test
    void select_shouldCacheNonNullButNotNull() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();

        AtomicInteger calls = new AtomicInteger();
        String v1 = node.callSelect(context, "k", () -> {
            calls.incrementAndGet();
            return "x";
        });
        String v2 = node.callSelect(context, "k", () -> {
            calls.incrementAndGet();
            return "y";
        });
        assertEquals("x", v1);
        assertEquals("x", v2);
        assertEquals(1, calls.get());

        AtomicInteger nullCalls = new AtomicInteger();
        Object n1 = node.callSelect(context, "knull", () -> {
            nullCalls.incrementAndGet();
            return null;
        });
        Object n2 = node.callSelect(context, "knull", () -> {
            nullCalls.incrementAndGet();
            return null;
        });
        assertNull(n1);
        assertNull(n2);
        assertEquals(2, nullCalls.get());
    }

    @Test
    void requireContext_missing_shouldThrowArgsError() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();

        assertThrows(CPReturnException.class, () -> node.callRequireContext(context, "k", String.class));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void requireBind_missing_shouldThrowArgsError() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();

        assertThrows(CPReturnException.class, () -> node.callRequireBind(context, "k", String.class));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    private static final class TestableNode extends CPNodeComponent {
        @Override
        public void process(team.carrypigeon.backend.api.bo.connection.CPSession session, CPFlowContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            return null;
        }

        private String callBuildSelectKey(String table, String field, Object value) {
            return buildSelectKey(table, field, value);
        }

        private String callBuildSelectKey(String table, Map<String, ?> fields) {
            return buildSelectKey(table, fields);
        }

        private <T> T callSelect(CPFlowContext context, String cacheKey, java.util.function.Supplier<T> fn) {
            return select(context, cacheKey, fn);
        }

        private <T> T callRequireContext(CPFlowContext context, String key, Class<T> type) throws team.carrypigeon.backend.api.chat.domain.controller.CPReturnException {
            return requireContext(context, key, type);
        }

        private <T> T callRequireBind(CPFlowContext context, String key, Class<T> type) throws team.carrypigeon.backend.api.chat.domain.controller.CPReturnException {
            return requireBind(context, key, type);
        }
    }
}
