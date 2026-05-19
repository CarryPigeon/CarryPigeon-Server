package example.plugin.messageplugin.mcbridge;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * Example specialized extension handler for mc-bridge.
 * It shows how an extension can layer custom payload validation on top of the generic extension handler.
 */
public class McBridgeMessagePlugin extends PluginChannelMessagePlugin {

    private final JsonProvider jsonProvider;

    public McBridgeMessagePlugin(JsonProvider jsonProvider) {
        super(McBridgeMessageTypePluginConfiguration.MESSAGE_TYPE, jsonProvider);
        this.jsonProvider = jsonProvider;
    }

    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof PluginChannelMessageDraft pluginDraft)) {
            throw new IllegalArgumentException("mc bridge plugin only supports PluginChannelMessageDraft");
        }
        JsonNode payloadNode = jsonProvider.readTree(pluginDraft.payload());
        JsonNode eventNode = payloadNode.get("event");
        if (eventNode == null || !eventNode.isTextual() || eventNode.asText().isBlank()) {
            throw ProblemException.validationFailed("mc-bridge event must not be blank");
        }
        return super.createMessage(context, draft);
    }
}
