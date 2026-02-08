package team.carrypigeon.backend.api.chat.domain.node;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CPNodeComponent} 基类单元测试。
 */
class CPNodeComponentTests {

    @Test
    void buildSelectKey_single_shouldFormatCorrectly() {
        TestableNode node = new TestableNode();

        assertEquals("user-id:id=123", node.callBuildSelectKey("user", "id", 123));
        assertEquals("channel-name:name=test", node.callBuildSelectKey("channel", "name", "test"));
    }

    @Test
    void buildSelectKey_single_shouldHandleNulls() {
        TestableNode node = new TestableNode();

        assertEquals("-:=null", node.callBuildSelectKey(null, null, null));
        assertEquals("t-:=null", node.callBuildSelectKey("t", null, null));
    }

    @Test
    void buildSelectKey_map_shouldSortKeysAlphabetically() {
        TestableNode node = new TestableNode();

        // 字段应按字母序排列
        assertEquals("member-cid_uid:cid=1;uid=2",
                node.callBuildSelectKey("member", Map.of("uid", 2, "cid", 1)));
        assertEquals("t-a_b_c:a=1;b=2;c=3",
                node.callBuildSelectKey("t", Map.of("c", 3, "a", 1, "b", 2)));
    }

    @Test
    void buildSelectKey_map_shouldHandleEmpty() {
        TestableNode node = new TestableNode();

        assertEquals("t-:", node.callBuildSelectKey("t", Map.of()));
        assertEquals("-:", node.callBuildSelectKey(null, null));
    }

    @Test
    void select_shouldCacheNonNullResults() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();
        AtomicInteger callCount = new AtomicInteger();

        // 第一次调用
        String v1 = node.callSelect(context, "key1", () -> {
            callCount.incrementAndGet();
            return "value";
        });

        // 第二次调用（应该命中缓存）
        String v2 = node.callSelect(context, "key1", () -> {
            callCount.incrementAndGet();
            return "different";
        });

        assertEquals("value", v1);
        assertEquals("value", v2);
        assertEquals(1, callCount.get(), "应该只调用一次查询函数");
    }

    @Test
    void select_shouldNotCacheNullResults() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();
        AtomicInteger callCount = new AtomicInteger();

        // null 结果不应缓存
        Object n1 = node.callSelect(context, "nullKey", () -> {
            callCount.incrementAndGet();
            return null;
        });
        Object n2 = node.callSelect(context, "nullKey", () -> {
            callCount.incrementAndGet();
            return null;
        });

        assertNull(n1);
        assertNull(n2);
        assertEquals(2, callCount.get(), "null 结果不应被缓存");
    }

    @Test
    void requireContext_shouldReturnValueWhenPresent() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();
        context.setData("key", "value");

        String result = node.callRequireContext(context, "key", String.class);

        assertEquals("value", result);
    }

    @Test
    void requireContext_shouldThrowWhenMissing() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();

        CPProblemException ex = assertThrows(CPProblemException.class,
                () -> node.callRequireContext(context, "missing", String.class));

        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void optionalContext_shouldReturnNullWhenMissing() {
        TestableNode node = new TestableNode();
        CPFlowContext context = new CPFlowContext();

        Object result = node.callOptionalContext(context, "missing");

        assertNull(result);
    }

    @Test
    void fail_shouldThrowCPProblemException() {
        TestableNode node = new TestableNode();

        CPProblemException ex = assertThrows(CPProblemException.class,
                () -> node.callFail(404, "not_found", "resource not found"));

        assertEquals(404, ex.getProblem().status());
        assertEquals("not_found", ex.getProblem().reason());
        assertEquals("resource not found", ex.getProblem().message());
    }

    @Test
    void validationFailed_shouldThrow422() {
        TestableNode node = new TestableNode();

        CPProblemException ex = assertThrows(CPProblemException.class,
                node::callValidationFailed);

        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void notFound_shouldThrow404() {
        TestableNode node = new TestableNode();

        CPProblemException ex = assertThrows(CPProblemException.class,
                () -> node.callNotFound("user not found"));

        assertEquals(404, ex.getProblem().status());
        assertEquals("not_found", ex.getProblem().reason());
    }

    @Test
    void forbidden_shouldThrow403() {
        TestableNode node = new TestableNode();

        CPProblemException ex = assertThrows(CPProblemException.class,
                () -> node.callForbidden("not_channel_member", "you are not a member"));

        assertEquals(403, ex.getProblem().status());
        assertEquals("not_channel_member", ex.getProblem().reason());
    }

    /**
     * 测试用的 CPNodeComponent 子类，暴露 protected 方法供测试。
     */
    private static final class TestableNode extends CPNodeComponent {

        @Override
        protected void process(CPFlowContext context) {
            throw new UnsupportedOperationException("测试类不实现 process");
        }

        @Override
        public String getNodeId() {
            return "TestableNode";
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            return null; // 测试用，不实现 bind 功能
        }

        String callBuildSelectKey(String table, String field, Object value) {
            return buildSelectKey(table, field, value);
        }

        String callBuildSelectKey(String table, Map<String, ?> fields) {
            return buildSelectKey(table, fields);
        }

        <T> T callSelect(CPFlowContext context, String cacheKey, Supplier<T> fn) {
            return select(context, cacheKey, fn);
        }

        <T> T callRequireContext(CPFlowContext context, String key, Class<T> type) {
            return requireContext(context, key, type);
        }

        <T> T callOptionalContext(CPFlowContext context, String key) {
            return optionalContext(context, key);
        }

        void callFail(int status, String reason, String message) {
            fail(team.carrypigeon.backend.api.chat.domain.error.CPProblem.of(status, reason, message));
        }

        void callValidationFailed() {
            validationFailed();
        }

        void callNotFound(String message) {
            notFound(message);
        }

        void callForbidden(String reason, String message) {
            forbidden(reason, message);
        }
    }
}
