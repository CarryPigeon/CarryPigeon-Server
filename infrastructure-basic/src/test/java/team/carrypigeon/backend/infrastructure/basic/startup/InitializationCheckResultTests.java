package team.carrypigeon.backend.infrastructure.basic.startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证初始化检查结果模型的最小契约。
 * 职责：确保共享启动检查结果能稳定表达通过与失败状态。
 * 边界：只验证结果模型本身，不验证具体检查实现。
 */
@Tag("unit")
class InitializationCheckResultTests {

    /**
     * 测试成功结果工厂方法。
     * 输入：成功说明。
     * 期望：结果被标记为通过并保留说明文本。
     */
    @Test
    void passed_withMessage_returnsPassedResult() {
        InitializationCheckResult result = InitializationCheckResult.passed("database ready");

        assertTrue(result.passed());
        assertEquals("database ready", result.message());
    }

    /**
     * 测试失败结果工厂方法。
     * 输入：失败说明。
     * 期望：结果被标记为失败并保留说明文本。
     */
    @Test
    void failed_withMessage_returnsFailedResult() {
        InitializationCheckResult result = InitializationCheckResult.failed("redis ping failed");

        assertFalse(result.passed());
        assertEquals("redis ping failed", result.message());
    }
}
