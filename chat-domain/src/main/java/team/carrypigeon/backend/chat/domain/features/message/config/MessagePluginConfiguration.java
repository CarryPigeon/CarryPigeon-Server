package team.carrypigeon.backend.chat.domain.features.message.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
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
    public ChannelMessagePlugin textChannelMessagePlugin() {
        return new TextChannelMessagePlugin();
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
    public ChannelMessagePlugin fileChannelMessagePlugin(
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
    public ChannelMessagePlugin voiceChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        return new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy);
    }

    /**
     * 创建消息插件注册器。
     *
     * @param plugins 当前运行时可用插件列表
     * @return 消息插件注册器
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(List<ChannelMessagePlugin> plugins) {
        return new ChannelMessagePluginRegistry(plugins);
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
