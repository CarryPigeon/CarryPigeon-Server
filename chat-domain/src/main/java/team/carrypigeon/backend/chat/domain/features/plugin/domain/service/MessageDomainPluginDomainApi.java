package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.MessageDomainPluginApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.command.ValidateMessageDataCommand;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.ValidatedMessageDataResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息 domain 插件 API 实现。
 * 职责：按 domain 分派插件并收敛客户端可发送约束。
 */
@Service
public class MessageDomainPluginDomainApi implements MessageDomainPluginApi {

    private final ChannelMessagePluginRegistry pluginRegistry;

    public MessageDomainPluginDomainApi(ChannelMessagePluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public ValidatedMessageDataResult validateMessageData(ValidateMessageDataCommand command) {
        ChannelMessagePlugin plugin = pluginRegistry.requireDomain(command.domain());
        if (command.clientRequest() && !plugin.clientSendable()) {
            throw ProblemException.validationFailed("domain is not client-sendable");
        }
        ChannelMessagePlugin.CanonicalData canonicalData = plugin.validateCanonicalData(
                new ChannelMessagePlugin.ChannelMessageBuildContext(
                        command.messageId(), command.channelId(), command.senderId(), command.sendTime()
                ),
                command.domainVersion(),
                command.data()
        );
        return new ValidatedMessageDataResult(
                plugin.supportedDomain(),
                command.domainVersion(),
                canonicalData.data(),
                canonicalData.preview()
        );
    }

    @Override
    public boolean supportsExtensionMessageType(String messageType) {
        return pluginRegistry.supportsExtensionMessageType(messageType);
    }
}
