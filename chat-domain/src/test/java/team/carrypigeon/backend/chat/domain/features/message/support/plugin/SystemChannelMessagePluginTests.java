package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SystemChannelMessagePlugin 契约测试。
 * 职责：验证 system 消息插件的最小摘要与 payload 生成语义。
 * 边界：只验证 system 插件自身规则，不覆盖内部发送编排。
 */
@Tag("contract")
class SystemChannelMessagePluginTests {

    /**
     * 验证 system 消息插件会生成稳定 system 预览文本。
     */
    @Test
    @DisplayName("create message valid system draft builds preview text")
    void createMessage_validSystemDraft_buildsPreviewText() {
        SystemChannelMessagePlugin plugin = new SystemChannelMessagePlugin(new JsonProvider(new ObjectMapper()));

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new SystemChannelMessageDraft("maintenance notice", "{\"severity\":\"info\"}", null)
        );

        assertEquals("system", message.messageType());
        assertEquals("maintenance notice", message.body());
        assertEquals("[系统消息] maintenance notice", message.previewText());
    }
}
