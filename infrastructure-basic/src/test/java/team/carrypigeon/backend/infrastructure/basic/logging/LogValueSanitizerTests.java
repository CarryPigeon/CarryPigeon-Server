package team.carrypigeon.backend.infrastructure.basic.logging;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogValueSanitizer 契约测试。
 * 职责：验证日志值的单行化、长度限制和敏感查询参数遮蔽规则。
 * 边界：不验证具体日志框架或 appender 行为。
 */
@Tag("unit")
class LogValueSanitizerTests {

    /**
     * 验证控制字符被替换，避免不可信文本伪造多行日志。
     */
    @Test
    void singleLine_controlCharacters_replacesWithSpaces() {
        assertEquals("first second third", LogValueSanitizer.singleLine("first\r\nsecond\tthird"));
    }

    /**
     * 验证超长输入被限制，避免日志字段无限膨胀。
     */
    @Test
    void singleLine_oversizedValue_truncatesWithMarker() {
        String sanitized = LogValueSanitizer.singleLine("a".repeat(600));

        assertEquals(512, sanitized.length());
        assertTrue(sanitized.endsWith("..."));
    }

    /**
     * 验证截断边界不会拆开 Unicode 代理对，避免生成无效日志文本。
     */
    @Test
    void singleLine_surrogatePairAtBoundary_keepsValidUnicode() {
        String sanitized = LogValueSanitizer.singleLine("a".repeat(508) + "😀" + "b".repeat(10));

        assertEquals("a".repeat(508) + "...", sanitized);
    }

    /**
     * 验证常见凭证参数和 URL 编码后的敏感参数名都会被遮蔽。
     */
    @Test
    void query_sensitiveAndEncodedNames_masksValues() {
        String sanitized = LogValueSanitizer.query(
                "plain=1&access_token=token-value&client_secret=secret-value&%61pi_key=key-value"
        );

        assertEquals("plain=1&access_token=***&client_secret=***&%61pi_key=***", sanitized);
        assertFalse(sanitized.contains("token-value"));
        assertFalse(sanitized.contains("secret-value"));
        assertFalse(sanitized.contains("key-value"));
    }

    /**
     * 验证畸形 URL 编码的参数名采用保守遮蔽，避免无法识别时泄露值。
     */
    @Test
    void query_malformedEncodedName_masksValue() {
        assertEquals("bad%name=***", LogValueSanitizer.query("bad%name=credential"));
    }
}
