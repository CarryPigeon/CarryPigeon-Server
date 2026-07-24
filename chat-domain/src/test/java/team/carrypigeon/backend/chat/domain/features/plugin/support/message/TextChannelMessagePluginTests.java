package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TextChannelMessagePlugin 契约测试。
 * 职责：验证文本消息插件的正文归一化与基础校验语义。
 * 边界：只验证 text 插件自身规则，不覆盖消息领域服务编排。
 */
@Tag("contract")
class TextChannelMessagePluginTests {

    /**
     * 验证文本消息插件会生成稳定正文与消息类型。
     */
    @Test
    @DisplayName("validate canonical data valid text builds normalized data")
    void validateCanonicalData_validText_buildsNormalizedData() {
        TextChannelMessagePlugin plugin = new TextChannelMessagePlugin();

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of("text", "  hello world  ")
        );

        assertEquals("hello world", canonical.data().get("text"));
        assertEquals("hello world", canonical.preview());
    }

    /**
     * 验证空白正文会返回稳定校验问题。
     */
    @Test
    @DisplayName("validate canonical data blank text throws validation problem")
    void validateCanonicalData_blankText_throwsValidationProblem() {
        TextChannelMessagePlugin plugin = new TextChannelMessagePlugin();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of("text", "   ")
                )
        );

        assertEquals("text must not be blank", exception.getMessage());
    }

    /**
     * 验证 canonical data 中非字符串正文会被插件拒绝。
     */
    @Test
    @DisplayName("validate canonical data non string text throws validation problem")
    void validateCanonicalData_nonStringText_throwsValidationProblem() {
        TextChannelMessagePlugin plugin = new TextChannelMessagePlugin();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of("text", 123)
                )
        );

        assertEquals("text must be string", exception.getMessage());
    }
}
