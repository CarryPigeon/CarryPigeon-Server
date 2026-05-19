package team.carrypigeon.backend.chat.domain.features.message.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息插件装配配置。
 * 职责：在 message feature 内装配消息插件注册器与共享基础 Bean。
 * 边界：具体消息类型插件由各自独立配置类声明，这里不再维护集中式注册大表。
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
     * 创建消息插件注册器。
     *
     * @param registrations 当前运行时可用的消息类型注册项
     * @return 消息插件注册器
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(java.util.List<ChannelMessagePluginRegistration> registrations) {
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
