package team.carrypigeon.backend.chat.domain.features.plugin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.CustomMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.FileMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.SystemMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.ReplyTextMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.TextMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.VoiceMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;
import team.carrypigeon.backend.chat.domain.support.TestFeatureApis;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PluginConfiguration 装配契约测试。
 * 职责：验证当前运行时插件治理注册是否保持与对象存储可用性一致。
 * 边界：不验证消息发送主链路，只验证 Spring 上下文中的插件装配结果。
 */
@Tag("contract")
class PluginConfigurationContextTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestSupportConfiguration.class,
                    PluginConfiguration.class,
                    TextMessageTypePluginConfiguration.class,
                    CustomMessageTypePluginConfiguration.class,
                    SystemMessageTypePluginConfiguration.class,
                    ReplyTextMessageTypePluginConfiguration.class,
                    FileMessageTypePluginConfiguration.class,
                    VoiceMessageTypePluginConfiguration.class
            );

    /**
     * 验证未提供对象存储时只公开 text 插件。
     */
    @Test
    @DisplayName("configuration without storage exposes text plugin only")
    void configuration_withoutStorage_exposesTextPluginOnly() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChannelMessagePluginRegistry.class);
            assertThat(context.getBean(ChannelMessagePluginRegistry.class).getPublicPluginKeys())
                    .containsExactly("custom", "text");
        });
    }

    /**
     * 验证通过配置关闭 custom 后，公开插件列表会收敛到 text。
     */
    @Test
    @DisplayName("configuration disables custom hides it from public plugins")
    void configuration_disablesCustom_hidesItFromPublicPlugins() {
        contextRunner
                .withPropertyValues(
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
        PluginConfiguration configuration = new PluginConfiguration();
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper());
        ObjectStorageService objectStorageService = new StorageSupportConfiguration().objectStorageService();
        PluginGovernanceProperties governanceProperties = new PluginGovernanceProperties();

        TextMessageTypePluginConfiguration textConfiguration = new TextMessageTypePluginConfiguration();
        CustomMessageTypePluginConfiguration customConfiguration = new CustomMessageTypePluginConfiguration();
        FileMessageTypePluginConfiguration fileConfiguration = new FileMessageTypePluginConfiguration();
        VoiceMessageTypePluginConfiguration voiceConfiguration = new VoiceMessageTypePluginConfiguration();

        TextChannelMessagePlugin textChannelMessagePlugin = textConfiguration.textChannelMessagePlugin();
        FileChannelMessagePlugin fileChannelMessagePlugin = fileConfiguration.fileChannelMessagePlugin(
                objectStorageService, TestFeatureApis.fileReferences()
        );
        VoiceChannelMessagePlugin voiceChannelMessagePlugin = voiceConfiguration.voiceChannelMessagePlugin(
                objectStorageService, TestFeatureApis.fileReferences()
        );

        ChannelMessagePluginRegistry registry = configuration.channelMessagePluginRegistry(java.util.List.of(
                textConfiguration.textChannelMessagePluginRegistration(governanceProperties, textChannelMessagePlugin),
                customConfiguration.customChannelMessagePluginRegistration(
                        governanceProperties,
                        customConfiguration.customChannelMessagePlugin()
                ),
                fileConfiguration.fileChannelMessagePluginRegistration(governanceProperties, objectProvider(fileChannelMessagePlugin)),
                voiceConfiguration.voiceChannelMessagePluginRegistration(governanceProperties, objectProvider(voiceChannelMessagePlugin))
        ).stream().filter(java.util.Objects::nonNull).toList());

        assertThat(registry.getPublicPluginKeys()).containsExactly("custom", "file", "text", "voice");
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
                            .containsExactly("custom", "text");
                });
    }

    /**
     * 对象存储测试支撑配置。
     * 职责：为插件装配测试提供可用的对象存储 Bean，使 file/voice 插件具备注册条件。
     */
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

    /**
     * 基础测试支撑配置。
     * 职责：提供插件配置测试所需的 JSON 基础设施 Bean。
     */
    @Configuration
    static class TestSupportConfiguration {

        @Bean
        JsonProvider jsonProvider() {
            return new JsonProvider(new ObjectMapper());
        }

        @Bean
        FileReferenceApi fileReferenceApi() {
            return TestFeatureApis.fileReferences();
        }
    }

    private static <T> org.springframework.beans.factory.ObjectProvider<T> objectProvider(T object) {
        return new org.springframework.beans.factory.ObjectProvider<>() {
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
