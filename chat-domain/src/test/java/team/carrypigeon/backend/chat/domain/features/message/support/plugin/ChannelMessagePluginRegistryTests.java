package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChannelMessagePluginRegistry 契约测试。
 * 职责：验证插件注册器的最小治理注册、公开投影和重复保护语义。
 * 边界：不验证 Spring 装配，只验证运行时注册表行为。
 */
@Tag("contract")
class ChannelMessagePluginRegistryTests {

    /**
     * 验证公开插件列表来自治理描述而非原始插件顺序。
     */
    @Test
    @DisplayName("registry public plugin keys returns sorted visible descriptors")
    void getPublicPluginKeys_returnsSortedVisibleDescriptors() {
        ChannelMessagePluginRegistry registry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-voice", "voice", "voice", true),
                registration("builtin-file", "file", "file", true),
                registration("builtin-text", "text", "text", true)
        ));

        assertEquals(List.of("file", "text", "voice"), registry.getPublicPluginKeys());
        assertEquals(3, registry.getDescriptors().size());
    }

    /**
     * 验证重复消息类型会被拒绝。
     */
    @Test
    @DisplayName("registry duplicate message type throws illegal state")
    void duplicateMessageType_throwsIllegalState() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ChannelMessagePluginRegistry(List.of(
                        registration("builtin-text-a", "text", "text-a", true),
                        registration("builtin-text-b", "text", "text-b", true)
                ))
        );

        assertEquals("duplicate channel message plugin type: text", exception.getMessage());
    }

    /**
     * 验证重复公开插件标识会被拒绝。
     */
    @Test
    @DisplayName("registry duplicate public plugin key throws illegal state")
    void duplicatePublicPluginKey_throwsIllegalState() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ChannelMessagePluginRegistry(List.of(
                        registration("builtin-file", "file", "shared", true),
                        registration("builtin-voice", "voice", "shared", true)
                ))
        );

        assertEquals("duplicate public plugin key: shared", exception.getMessage());
    }

    private ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            boolean publicVisible
    ) {
        ChannelMessagePlugin plugin = new ChannelMessagePlugin() {
            @Override
            public String supportedType() {
                return messageType;
            }

            @Override
            public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
                throw new UnsupportedOperationException("test plugin");
            }
        };
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        publicPluginKey,
                        "test descriptor",
                        publicVisible,
                        List.of("message.sent"),
                        List.of("message:" + messageType + ":send"),
                        "test_condition"
                ),
                plugin
        );
    }
}
