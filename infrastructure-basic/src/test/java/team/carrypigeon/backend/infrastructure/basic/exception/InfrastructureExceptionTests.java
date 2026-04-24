package team.carrypigeon.backend.infrastructure.basic.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 验证基础设施异常的最小契约。
 * 职责：确保异常能稳定携带错误码和原始 cause。
 * 边界：不验证业务异常语义。
 */
@Tag("unit")
class InfrastructureExceptionTests {

    /**
     * 测试带错误码和 cause 的异常构造。
     * 输入：基础设施错误码、错误消息、原始异常。
     * 期望：异常对象保留错误码和原始 cause，便于上层识别与日志追踪。
     */
    @Test
    void constructor_withErrorCode_retainsErrorCodeAndCause() {
        RuntimeException cause = new RuntimeException("cause");

        InfrastructureException exception = new InfrastructureException(
                InfrastructureErrorCode.JSON_SERIALIZE_FAILED,
                "failed",
                cause
        );

        assertEquals(InfrastructureErrorCode.JSON_SERIALIZE_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }
}
