package team.carrypigeon.backend.infrastructure.basic.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureErrorCode;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证配置基础工具的最小契约。
 * 职责：确保基础配置校验能区分有效文本和空白文本。
 * 边界：不验证 Spring 配置绑定流程，只验证项目侧基础校验行为。
 */
@Tag("unit")
class ConfigurationSupportTests {

    /**
     * 测试非空文本配置值。
     * 输入：包含可见字符的字符串。
     * 期望：配置校验通过，不抛出异常。
     */
    @Test
    void requireNonBlank_hasText_passes() {
        assertDoesNotThrow(() -> ConfigurationSupport.requireNonBlank("value", "field"));
    }

    /**
     * 测试空白配置值。
     * 输入：仅包含空白字符的字符串。
     * 期望：抛出基础设施异常，并携带 CONFIG_BIND_FAILED 错误码。
     */
    @Test
    void requireNonBlank_blank_throwsConfigBindFailed() {
        InfrastructureException exception = assertThrows(
                InfrastructureException.class,
                () -> ConfigurationSupport.requireNonBlank(" ", "field")
        );

        assertEquals(InfrastructureErrorCode.CONFIG_BIND_FAILED, exception.getErrorCode());
    }
}
