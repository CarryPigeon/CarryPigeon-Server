package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginGovernanceProperties;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 语音消息类型插件配置。
 * 职责：声明 voice 消息类型的处理器与注册项。
 * 边界：只负责 voice 消息类型的 Spring 装配，不承载其它消息类型规则。
 */
@Configuration
public class VoiceMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "voice";

    /**
     * 创建语音消息插件处理器。
     *
     * @param objectStorageService 对象存储服务
     * @param jsonProvider 项目统一 JSON 门面
     * @param messageAttachmentObjectKeyPolicy 附件 objectKey 作用域策略
     * @return 语音消息插件处理器
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
     * 创建语音消息注册项。
     *
     * @param governanceProperties 消息插件治理配置
     * @param voiceChannelMessagePlugin 语音消息插件处理器
     * @return voice 消息注册项；若关闭则返回 null
     */
    @Bean
    public ChannelMessagePluginRegistration voiceChannelMessagePluginRegistration(
            MessagePluginGovernanceProperties governanceProperties,
            ObjectProvider<VoiceChannelMessagePlugin> voiceChannelMessagePluginProvider
    ) {
        VoiceChannelMessagePlugin voiceChannelMessagePlugin = voiceChannelMessagePluginProvider.getIfAvailable();
        if (voiceChannelMessagePlugin == null) {
            return null;
        }
        if (!governanceProperties.voiceEnabled()) {
            return null;
        }
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-voice-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in voice channel message plugin",
                true,
                java.util.List.of("message:voice:send"),
                "requires_object_storage",
                voiceChannelMessagePlugin
        );
    }
}
