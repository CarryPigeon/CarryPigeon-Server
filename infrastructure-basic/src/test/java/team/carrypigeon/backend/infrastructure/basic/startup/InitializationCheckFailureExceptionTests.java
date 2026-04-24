package team.carrypigeon.backend.infrastructure.basic.startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证初始化检查失败异常的最小契约。
 * 职责：确保启动期阻塞异常能输出稳定、可定位的信息。
 * 边界：不验证异常传播链，只验证消息语义。
 */
@Tag("unit")
class InitializationCheckFailureExceptionTests {

    /**
     * 测试异常消息格式。
     * 输入：检查名称与失败说明。
     * 期望：异常消息包含检查名称和失败原因。
     */
    @Test
    void constructor_withCheckNameAndMessage_formatsExceptionMessage() {
        InitializationCheckFailureException exception = new InitializationCheckFailureException(
                "database",
                "health query failed"
        );

        assertEquals(
                "Initialization check failed [database]: health query failed",
                exception.getMessage()
        );
    }
}
