package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.CustomChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CustomChannelMessagePlugin 契约测试。
 * 职责：验证自定义消息插件的结构化载荷与摘要生成语义。
 * 边界：只验证 custom 消息插件自身规则，不覆盖应用服务编排。
 */
@Tag("contract")
class CustomChannelMessagePluginTests {

    /**
     * 验证自定义消息插件会生成稳定摘要和 payload。
     */
    @Test
    @DisplayName("create message valid custom draft builds preview and payload")
    void createMessage_validCustomDraft_buildsPreviewAndPayload() {
        CustomChannelMessagePlugin plugin = new CustomChannelMessagePlugin(new JsonProvider(new ObjectMapper()));

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new CustomChannelMessageDraft("rich card", "{\"card\":\"status\"}", null)
        );

        assertEquals("custom", message.messageType());
        assertEquals("rich card", message.body());
        assertEquals("[自定义消息] rich card", message.previewText());
    }

    /**
     * 验证缺少 payload 时返回稳定校验问题。
     */
    @Test
    @DisplayName("create message blank custom payload throws validation problem")
    void createMessage_blankCustomPayload_throwsValidationProblem() {
        CustomChannelMessagePlugin plugin = new CustomChannelMessagePlugin(new JsonProvider(new ObjectMapper()));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new CustomChannelMessageDraft("rich card", " ", null)
                )
        );

        assertEquals("payload must not be blank", exception.getMessage());
    }
}
