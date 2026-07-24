package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.CanonicalData;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    /**
     * 验证扩展消息白名单只接受已注册的非内建 public key。
     */
    @Test
    @DisplayName("registry supports extension message type only for registered non builtin public key")
    void supportsExtensionMessageType_onlyForRegisteredNonBuiltinPublicKey() {
        ChannelMessagePluginRegistry registry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text", "text", "text", true),
                registration("example-extension", "test-extension", "test-extension", true)
        ));

        assertFalse(registry.supportsExtensionMessageType("text"));
        assertTrue(registry.supportsExtensionMessageType("test-extension"));
        assertFalse(registry.supportsExtensionMessageType("missing-extension"));
    }

    /**
     * 验证 canonical HTTP domain 会解析到负责校验 data 的插件。
     */
    @Test
    @DisplayName("registry require domain returns matching validator plugin")
    void requireDomain_registeredDomain_returnsMatchingPlugin() {
        ChannelMessagePluginRegistry registry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text", "text", "text", true)
        ));

        assertEquals("Core:text", registry.requireDomain("Core:text").supportedDomain());
        assertTrue(registry.supportsDomain("Core:text"));
        assertFalse(registry.supportsDomain("Core:missing"));
    }

    /**
     * 验证同一 canonical domain 不能由多个插件同时声明。
     */
    @Test
    @DisplayName("registry duplicate domain throws illegal state")
    void duplicateDomain_throwsIllegalState() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ChannelMessagePluginRegistry(List.of(
                        registration("plugin-a", "type-a", "a", true, "Core:shared"),
                        registration("plugin-b", "type-b", "b", true, "Core:shared")
                ))
        );

        assertEquals("duplicate channel message plugin domain: Core:shared", exception.getMessage());
    }

    /**
     * 验证未注册 domain 会在进入插件和存储前返回校验错误。
     */
    @Test
    @DisplayName("registry unknown domain throws validation problem")
    void requireDomain_unknownDomain_throwsValidationProblem() {
        ChannelMessagePluginRegistry registry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text", "text", "text", true)
        ));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> registry.requireDomain("Core:missing")
        );

        assertEquals("domain is not supported", exception.getMessage());
    }

    private ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            boolean publicVisible
    ) {
        return registration(pluginKey, messageType, publicPluginKey, publicVisible, "Core:" + messageType);
    }

    private ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            boolean publicVisible,
            String domain
    ) {
        ChannelMessagePlugin plugin = new ChannelMessagePlugin() {
            @Override
            public String supportedType() {
                return messageType;
            }

            @Override
            public String supportedDomain() {
                return domain;
            }

            @Override
            public CanonicalData validateCanonicalData(
                    ChannelMessageBuildContext context,
                    String domainVersion,
                    Map<String, Object> data
            ) {
                return new CanonicalData(data, "test");
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
