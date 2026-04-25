package team.carrypigeon.backend.chat.domain.features.message.config;

import java.util.List;
import java.util.ArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.CustomChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.SystemChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息插件装配配置。
 * 职责：在 message feature 内装配当前可用的消息插件与注册器。
 * 边界：这里只负责插件 Bean 声明，不承载消息业务编排。
 */
@Configuration
@EnableConfigurationProperties(MessagePluginGovernanceProperties.class)
public class MessagePluginConfiguration {

    /**
     * 创建消息附件 objectKey 规则策略。
     *
     * @return message feature 本地 objectKey 规则策略
     */
    @Bean
    public MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy() {
        return new MessageAttachmentObjectKeyPolicy();
    }

    /**
     * 创建文本消息插件。
     *
     * @return 文本消息插件
     */
    @Bean
    public TextChannelMessagePlugin textChannelMessagePlugin() {
        return new TextChannelMessagePlugin();
    }

    /**
     * 创建插件消息插件。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 插件消息插件
     */
    @Bean
    public PluginChannelMessagePlugin pluginChannelMessagePlugin(JsonProvider jsonProvider) {
        return new PluginChannelMessagePlugin(jsonProvider);
    }

    /**
     * 创建自定义消息插件。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 自定义消息插件
     */
    @Bean
    public CustomChannelMessagePlugin customChannelMessagePlugin(JsonProvider jsonProvider) {
        return new CustomChannelMessagePlugin(jsonProvider);
    }

    /**
     * 创建 system 消息插件。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return system 消息插件
     */
    @Bean
    public SystemChannelMessagePlugin systemChannelMessagePlugin(JsonProvider jsonProvider) {
        return new SystemChannelMessagePlugin(jsonProvider);
    }

    /**
     * 创建文件消息插件。
     *
     * @param objectStorageService 对象存储服务
     * @param jsonProvider 项目统一 JSON 门面
     * @return 文件消息插件
     */
    @Bean
    @ConditionalOnBean(ObjectStorageService.class)
    public FileChannelMessagePlugin fileChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        return new FileChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy);
    }

    /**
     * 创建语音消息插件。
     *
     * @param objectStorageService 对象存储服务
     * @param jsonProvider 项目统一 JSON 门面
     * @return 语音消息插件
     */
    @Bean
    @ConditionalOnBean(ObjectStorageService.class)
    public VoiceChannelMessagePlugin voiceChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        return new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy);
    }

    /**
     * 创建消息插件注册器。
     *
     * @param textChannelMessagePlugin 文本消息插件
     * @param fileChannelMessagePluginProvider 文件消息插件提供器
     * @param voiceChannelMessagePluginProvider 语音消息插件提供器
     * @return 消息插件注册器
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(
            TextChannelMessagePlugin textChannelMessagePlugin,
            PluginChannelMessagePlugin pluginChannelMessagePlugin,
            CustomChannelMessagePlugin customChannelMessagePlugin,
            SystemChannelMessagePlugin systemChannelMessagePlugin,
            MessagePluginGovernanceProperties governanceProperties,
            ObjectProvider<FileChannelMessagePlugin> fileChannelMessagePluginProvider,
            ObjectProvider<VoiceChannelMessagePlugin> voiceChannelMessagePluginProvider
    ) {
        List<ChannelMessagePluginRegistration> registrations = new ArrayList<>();
        registrations.add(new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        "builtin-text-message",
                        "text",
                        "text",
                        "Built-in text channel message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:text:send"),
                        "always_available"
                ),
                textChannelMessagePlugin
        ));
        if (governanceProperties.pluginEnabled()) {
            registrations.add(new ChannelMessagePluginRegistration(
                    new ChannelMessagePluginDescriptor(
                            "builtin-plugin-message",
                            "plugin",
                            "plugin",
                            "Built-in plugin channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:plugin:send"),
                            "always_available"
                    ),
                    pluginChannelMessagePlugin
            ));
        }
        if (governanceProperties.customEnabled()) {
            registrations.add(new ChannelMessagePluginRegistration(
                    new ChannelMessagePluginDescriptor(
                            "builtin-custom-message",
                            "custom",
                            "custom",
                            "Built-in custom channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:custom:send"),
                            "always_available"
                    ),
                    customChannelMessagePlugin
            ));
        }
        if (governanceProperties.systemEnabled()) {
            registrations.add(new ChannelMessagePluginRegistration(
                    new ChannelMessagePluginDescriptor(
                            "builtin-system-message",
                            "system",
                            "system",
                            "Built-in system channel message plugin",
                            false,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:system:send"),
                            "internal_only"
                    ),
                    systemChannelMessagePlugin
            ));
        }
        FileChannelMessagePlugin fileChannelMessagePlugin = fileChannelMessagePluginProvider.getIfAvailable();
        if (fileChannelMessagePlugin != null && governanceProperties.fileEnabled()) {
            registrations.add(new ChannelMessagePluginRegistration(
                    new ChannelMessagePluginDescriptor(
                            "builtin-file-message",
                            "file",
                            "file",
                            "Built-in file channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:file:send"),
                            "requires_object_storage"
                    ),
                    fileChannelMessagePlugin
            ));
        }
        VoiceChannelMessagePlugin voiceChannelMessagePlugin = voiceChannelMessagePluginProvider.getIfAvailable();
        if (voiceChannelMessagePlugin != null && governanceProperties.voiceEnabled()) {
            registrations.add(new ChannelMessagePluginRegistration(
                    new ChannelMessagePluginDescriptor(
                            "builtin-voice-message",
                            "voice",
                            "voice",
                            "Built-in voice channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:voice:send"),
                            "requires_object_storage"
                    ),
                    voiceChannelMessagePlugin
            ));
        }
        return new ChannelMessagePluginRegistry(registrations);
    }

    /**
     * 创建消息附件出站载荷解析器。
     *
     * @param objectStorageServiceProvider 对象存储服务提供器
     * @param jsonProvider 项目统一 JSON 门面
     * @return 消息附件出站载荷解析器
     */
    @Bean
    public MessageAttachmentPayloadResolver messageAttachmentPayloadResolver(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            JsonProvider jsonProvider
    ) {
        return new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider);
    }
}
