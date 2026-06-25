package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MessagePluginCatalogItemResult;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;

/**
 * 消息插件目录应用服务。
 * 职责：为其它 feature 提供稳定的消息插件目录查询入口。
 * 边界：只暴露目录快照与能力判断，不泄露 message feature 的 support 实现细节。
 */
@Service
public class MessagePluginCatalogApplicationService {

    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;

    public MessagePluginCatalogApplicationService(ChannelMessagePluginRegistry channelMessagePluginRegistry) {
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
    }

    /**
     * 返回当前运行时公开插件目录。
     *
     * @return 可公开暴露的插件目录项
     */
    public List<MessagePluginCatalogItemResult> listPublicPlugins() {
        return channelMessagePluginRegistry.getDescriptors().stream()
                .filter(descriptor -> descriptor.publicVisible())
                .map(descriptor -> new MessagePluginCatalogItemResult(
                        descriptor.messageType(),
                        descriptor.publicPluginKey(),
                        descriptor.description(),
                        descriptor.declaredPermissions()
                ))
                .toList();
    }

    /**
     * 判断扩展消息类型是否在当前运行时白名单中。
     *
     * @param messageType 扩展消息类型
     * @return 白名单命中时返回 true
     */
    public boolean supportsExtensionMessageType(String messageType) {
        return channelMessagePluginRegistry.supportsExtensionMessageType(messageType);
    }
}
