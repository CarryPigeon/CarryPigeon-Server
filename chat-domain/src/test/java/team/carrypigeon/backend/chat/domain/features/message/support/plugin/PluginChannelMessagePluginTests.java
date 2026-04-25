package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PluginChannelMessagePlugin 契约测试。
 * 职责：验证插件消息插件的结构化载荷与插件标识校验语义。
 * 边界：只验证 plugin 消息插件自身规则，不覆盖应用服务编排。
 */
@Tag("contract")
class PluginChannelMessagePluginTests {

    /**
     * 验证插件消息插件会生成稳定摘要和 canonical payload。
     */
    @Test
    @DisplayName("create message valid plugin draft builds canonical plugin payload")
    void createMessage_validPluginDraft_buildsCanonicalPluginPayload() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin(new JsonProvider(new ObjectMapper()));

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new PluginChannelMessageDraft("mc server bridge", "mc-bridge", "{\"event\":\"player_join\"}", null)
        );

        assertEquals("plugin", message.messageType());
        assertEquals("mc server bridge", message.body());
        assertEquals("[插件消息] mc server bridge", message.previewText());
    }

    /**
     * 验证缺少 pluginKey 时返回稳定校验问题。
     */
    @Test
    @DisplayName("create message blank plugin key throws validation problem")
    void createMessage_blankPluginKey_throwsValidationProblem() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin(new JsonProvider(new ObjectMapper()));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new PluginChannelMessageDraft("bridge", " ", "{\"event\":\"player_join\"}", null)
                )
        );

        assertEquals("pluginKey must not be blank", exception.getMessage());
    }
}
