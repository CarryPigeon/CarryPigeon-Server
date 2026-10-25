package example.plugin.messageplugin.mcbridge;

import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.MessageHandler;

/**
 * Example specialized message handler for mc-bridge.
 * It shows how a plugin can validate and normalize its own domain payload.
 */
@MessageHandler("Example:McBridge:V1")
public class McBridgeMessagePlugin implements CPChannelMessageHandler {

    @Override
    public CPChannelMessage handle(CPChannelMessage message) {
        Object payloadValue = message.data().get("payload");
        if (!(payloadValue instanceof Map<?, ?> payload)) {
            throw ProblemException.of(McBridgeProblem.PAYLOAD_NOT_OBJECT);
        }
        Object eventValue = payload.get("event");
        if (!(eventValue instanceof String event) || event.isBlank()) {
            throw ProblemException.of(McBridgeProblem.EVENT_BLANK);
        }
        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        payload.forEach((key, value) -> normalizedPayload.put((String) key, value));
        normalizedPayload.put("event", event.trim());
        Map<String, Object> normalizedData = new LinkedHashMap<>(message.data());
        normalizedData.put("payload", normalizedPayload);
        String text = message.data().get("text") instanceof String value && !value.isBlank()
                ? value
                : "A player joined";
        return new CPChannelMessage(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                normalizedData,
                message.sendTime(),
                message.mentions(),
                text,
                message.status()
        );
    }
}
