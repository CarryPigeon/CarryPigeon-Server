package team.carrypigeon.backend.chat.domain.features.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessagePluginConfiguration 装配契约测试。
 * 职责：验证当前运行时插件治理注册是否保持与对象存储可用性一致。
 * 边界：不验证消息发送主链路，只验证 Spring 上下文中的插件装配结果。
 */
@Tag("contract")
class MessagePluginConfigurationContextTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestSupportConfiguration.class, MessagePluginConfiguration.class);

    /**
     * 验证未提供对象存储时只公开 text 插件。
     */
    @Test
    @DisplayName("configuration without storage exposes text plugin only")
    void configuration_withoutStorage_exposesTextPluginOnly() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChannelMessagePluginRegistry.class);
            assertThat(context.getBean(ChannelMessagePluginRegistry.class).getPublicPluginKeys())
                    .containsExactly("custom", "plugin", "text");
        });
    }

    /**
     * 验证通过配置关闭 plugin/custom 后，公开插件列表会收敛到 text。
     */
    @Test
    @DisplayName("configuration disables plugin and custom hides them from public plugins")
    void configuration_disablesPluginAndCustom_hidesThemFromPublicPlugins() {
        contextRunner
                .withPropertyValues(
                        "cp.chat.message.plugins.plugin-enabled=false",
                        "cp.chat.message.plugins.custom-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ChannelMessagePluginRegistry.class);
                    assertThat(context.getBean(ChannelMessagePluginRegistry.class).getPublicPluginKeys())
                            .containsExactly("text");
                });
    }

    /**
     * 验证提供对象存储后会公开 text、file、voice 插件。
     */
    @Test
    @DisplayName("configuration with storage exposes text file and voice plugins")
    void configuration_withStorage_exposesTextFileAndVoicePlugins() {
        MessagePluginConfiguration configuration = new MessagePluginConfiguration();
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper());
        ObjectStorageService objectStorageService = new StorageSupportConfiguration().objectStorageService();
        ChannelMessagePluginRegistry registry = configuration.channelMessagePluginRegistry(
                configuration.textChannelMessagePlugin(),
                configuration.pluginChannelMessagePlugin(jsonProvider),
                configuration.customChannelMessagePlugin(jsonProvider),
                configuration.systemChannelMessagePlugin(jsonProvider),
                new MessagePluginGovernanceProperties(),
                objectProvider(configuration.fileChannelMessagePlugin(objectStorageService, jsonProvider, configuration.messageAttachmentObjectKeyPolicy())),
                objectProvider(configuration.voiceChannelMessagePlugin(objectStorageService, jsonProvider, configuration.messageAttachmentObjectKeyPolicy()))
        );

        assertThat(registry.getPublicPluginKeys()).containsExactly("custom", "file", "plugin", "text", "voice");
    }

    /**
     * 验证通过配置关闭 file/voice 后，即使对象存储存在也不会公开这些插件。
     */
    @Test
    @DisplayName("configuration disables file and voice hides storage backed plugins")
    void configuration_disablesFileAndVoice_hidesStorageBackedPlugins() {
        contextRunner
                .withPropertyValues(
                        "cp.chat.message.plugins.file-enabled=false",
                        "cp.chat.message.plugins.voice-enabled=false"
                )
                .withUserConfiguration(StorageSupportConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChannelMessagePluginRegistry.class);
                    assertThat(context.getBean(ChannelMessagePluginRegistry.class).getPublicPluginKeys())
                            .containsExactly("custom", "plugin", "text");
                });
    }

    @Configuration
    static class StorageSupportConfiguration {

        @Bean
        ObjectStorageService objectStorageService() {
            return new ObjectStorageService() {
                @Override
                public StorageObject put(PutObjectCommand command) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<StorageObject> get(GetObjectCommand command) {
                    return Optional.of(StorageObject.metadata(command.objectKey(), "application/octet-stream", 1L));
                }

                @Override
                public void delete(team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand command) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Configuration
    static class TestSupportConfiguration {

        @Bean
        JsonProvider jsonProvider() {
            return new JsonProvider(new ObjectMapper());
        }
    }

    private static <T> ObjectProvider<T> objectProvider(T object) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return object;
            }

            @Override
            public T getIfAvailable() {
                return object;
            }

            @Override
            public T getIfUnique() {
                return object;
            }

            @Override
            public T getObject() {
                return object;
            }
        };
    }
}
