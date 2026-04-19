package team.carrypigeon.backend.infrastructure.basic.logging;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 MDC 日志上下文工具的最小契约。
 * 职责：确保统一日志字段能被写入、忽略、移除和清理。
 * 边界：不验证 Log4j2 输出格式，只验证 MDC 操作契约。
 */
class LogContextsTests {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * 测试 traceId 便捷写入。
     * 输入：traceId 文本。
     * 期望：MDC 中写入统一 trace_id 字段。
     */
    @Test
    void traceId_value_putsTraceIdKey() {
        LogContexts.traceId("trace-1");

        assertEquals("trace-1", MDC.get(LogKeys.TRACE_ID));
    }

    /**
     * 测试空白值写入。
     * 输入：空白字符串。
     * 期望：MDC 不写入对应字段，避免日志上下文污染。
     */
    @Test
    void put_blankValue_ignored() {
        LogContexts.put(LogKeys.UID, " ");

        assertNull(MDC.get(LogKeys.UID));
    }

    /**
     * 测试批量写入。
     * 输入：包含 request_id 与 route 的 Map。
     * 期望：MDC 中同时存在两个统一字段。
     */
    @Test
    void putAll_values_putsAllKeys() {
        LogContexts.putAll(Map.of(LogKeys.REQUEST_ID, "req-1", LogKeys.ROUTE, "/api/server"));

        assertEquals("req-1", MDC.get(LogKeys.REQUEST_ID));
        assertEquals("/api/server", MDC.get(LogKeys.ROUTE));
    }

    /**
     * 测试字段移除。
     * 输入：已写入的 uid 字段。
     * 期望：remove 后 MDC 中不再存在 uid。
     */
    @Test
    void remove_existingKey_removesValue() {
        LogContexts.uid("100");

        LogContexts.remove(LogKeys.UID);

        assertNull(MDC.get(LogKeys.UID));
    }
}
