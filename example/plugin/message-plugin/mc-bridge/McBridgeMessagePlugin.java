package example.plugin.messageplugin.mcbridge;

import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * Example specialized extension handler for mc-bridge.
 * It shows how an extension can layer custom payload validation on top of the generic extension handler.
 */
public class McBridgeMessagePlugin extends PluginChannelMessagePlugin {

    public McBridgeMessagePlugin() {
        super(McBridgeMessageTypePluginConfiguration.MESSAGE_TYPE);
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        CanonicalData canonicalData = super.validateCanonicalData(context, domainVersion, rawData);
        Object payloadValue = canonicalData.data().get("payload");
        if (!(payloadValue instanceof Map<?, ?> payload)) {
            throw ProblemException.validationFailed("mc-bridge payload must be object");
        }
        Object eventValue = payload.get("event");
        if (!(eventValue instanceof String event) || event.isBlank()) {
            throw ProblemException.validationFailed("mc-bridge event must not be blank");
        }
        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        payload.forEach((key, value) -> normalizedPayload.put((String) key, value));
        normalizedPayload.put("event", event.trim());
        Map<String, Object> normalizedData = new LinkedHashMap<>(canonicalData.data());
        normalizedData.put("payload", normalizedPayload);
        return new CanonicalData(normalizedData, canonicalData.preview());
    }
}
