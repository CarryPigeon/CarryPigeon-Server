package team.carrypigeon.backend.infrastructure.basic.startup;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * InitializationCheckRunner 契约测试。
 * 职责：验证启动检查对通过、结果失败和直接异常的统一处理语义。
 * 边界：不连接外部服务，也不验证具体日志框架输出格式。
 */
@Tag("unit")
class InitializationCheckerRunnerTests {

    /**
     * 验证全部检查通过时启动检查执行器正常结束。
     */
    @Test
    @DisplayName("after singletons instantiated passed check completes")
    void afterSingletonsInstantiated_passedCheck_completes() {
        InitializationCheckRunner runner = runner(true, () -> InitializationCheckResult.passed("ready"));

        assertDoesNotThrow(runner::afterSingletonsInstantiated);
    }

    /**
     * 验证必需检查返回失败结果时会阻止启动并保留稳定检查名。
     */
    @Test
    @DisplayName("after singletons instantiated required failure throws startup failure")
    void afterSingletonsInstantiated_requiredFailure_throwsStartupFailure() {
        InitializationCheckRunner runner = runner(true, () -> InitializationCheckResult.failed("service unavailable"));

        InitializationCheckFailureException exception = assertThrows(
                InitializationCheckFailureException.class,
                runner::afterSingletonsInstantiated
        );

        assertEquals("Initialization check failed [test-check]: service unavailable", exception.getMessage());
    }

    /**
     * 验证可选检查返回失败结果时不会阻止启动。
     */
    @Test
    @DisplayName("after singletons instantiated optional failure continues")
    void afterSingletonsInstantiated_optionalFailure_continues() {
        InitializationCheckRunner runner = runner(false, () -> InitializationCheckResult.failed("service unavailable"));

        assertDoesNotThrow(runner::afterSingletonsInstantiated);
    }

    /**
     * 验证必需检查直接抛出的异常会被收口为稳定启动失败，并清理控制字符。
     */
    @Test
    @DisplayName("after singletons instantiated required exception wraps startup failure")
    void afterSingletonsInstantiated_requiredException_wrapsStartupFailure() {
        InitializationCheckRunner runner = runner(true, () -> {
            throw new IllegalStateException("connection\r\nfailed");
        });

        InitializationCheckFailureException exception = assertThrows(
                InitializationCheckFailureException.class,
                runner::afterSingletonsInstantiated
        );

        assertEquals(
                "Initialization check failed [test-check]: IllegalStateException: connection failed",
                exception.getMessage()
        );
    }

    private InitializationCheckRunner runner(
            boolean required,
            Supplier<InitializationCheckResult> resultSupplier
    ) {
        InitializationChecker check = new InitializationChecker() {
            @Override
            public String name() {
                return "test-check";
            }

            @Override
            public boolean required() {
                return required;
            }

            @Override
            public InitializationCheckResult check() {
                return resultSupplier.get();
            }
        };
        return new InitializationCheckRunner(List.of(check));
    }
}
