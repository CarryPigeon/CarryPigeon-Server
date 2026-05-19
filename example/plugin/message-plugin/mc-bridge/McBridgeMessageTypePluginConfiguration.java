package example.plugin.messageplugin.mcbridge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginGovernanceProperties;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.MessageTypePluginRegistrationSupport;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * Example configuration for the mc-bridge extension type.
 * This file is explanatory sample code and is not wired into the production runtime.
 */
@Configuration
public class McBridgeMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "mc-bridge";

    @Bean
    public McBridgeMessagePlugin mcBridgeMessagePlugin(JsonProvider jsonProvider) {
        return new McBridgeMessagePlugin(jsonProvider);
    }

    @Bean
    public ChannelMessagePluginRegistration mcBridgeMessagePluginRegistration(
            MessagePluginGovernanceProperties governanceProperties,
            McBridgeMessagePlugin mcBridgeMessagePlugin
    ) {
        if (!governanceProperties.pluginEnabled()) {
            return null;
        }
        return MessageTypePluginRegistrationSupport.registration(
                "example-mc-bridge-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Example mc bridge extension message plugin",
                true,
                java.util.List.of("message:mc-bridge:send"),
                "always_available",
                mcBridgeMessagePlugin
        );
    }
}
