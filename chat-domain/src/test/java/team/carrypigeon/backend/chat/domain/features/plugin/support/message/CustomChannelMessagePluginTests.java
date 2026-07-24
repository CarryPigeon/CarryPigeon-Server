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
 * CustomChannelMessagePlugin 契约测试。
 * 职责：验证自定义消息插件的结构化载荷与摘要生成语义。
 * 边界：只验证 custom 消息插件自身规则，不覆盖领域服务编排。
 */
@Tag("contract")
class CustomChannelMessagePluginTests {

    /**
     * 验证自定义消息插件会生成稳定摘要和 payload。
     */
    @Test
    @DisplayName("validate canonical data valid custom data builds preview")
    void validateCanonicalData_validCustomData_buildsPreview() {
        CustomChannelMessagePlugin plugin = new CustomChannelMessagePlugin();

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of("text", "rich card", "card", "status")
        );

        assertEquals("rich card", canonical.data().get("text"));
        assertEquals("status", canonical.data().get("card"));
        assertEquals("[自定义消息] rich card", canonical.preview());
    }

    /**
     * 验证缺少 payload 时返回稳定校验问题。
     */
    @Test
    @DisplayName("validate canonical data invalid metadata throws validation problem")
    void validateCanonicalData_invalidMetadata_throwsValidationProblem() {
        CustomChannelMessagePlugin plugin = new CustomChannelMessagePlugin();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of("text", "rich card", "metadata", "invalid")
                )
        );

        assertEquals("metadata must be object", exception.getMessage());
    }
}
