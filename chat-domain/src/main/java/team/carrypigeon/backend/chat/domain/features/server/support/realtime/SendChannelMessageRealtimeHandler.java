package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 频道消息实时入站处理器。
 * 职责：承接实时通道消息发送命令并转换为应用层草稿。
 * 边界：HTTP 不提供消息发送入口；实时通道是消息发送的唯一外部入口。
 */
public class SendChannelMessageRealtimeHandler implements RealtimeInboundMessageHandler {

    private static final String GENERIC_SEND_COMMAND = "send_channel_message";
    private static final String TEXT_MESSAGE_TYPE = "text";
    private static final String FILE_MESSAGE_TYPE = "file";
    private static final String VOICE_MESSAGE_TYPE = "voice";

    private final JsonProvider jsonProvider;

    public SendChannelMessageRealtimeHandler(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public boolean supports(RealtimeClientMessage request) {
        return request != null && GENERIC_SEND_COMMAND.equals(request.type());
    }

    @Override
    public void handle(
            AuthenticatedPrincipal principal,
            RealtimeClientMessage request,
            MessageApplicationService messageApplicationService
    ) {
        messageApplicationService.sendChannelMessage(new SendChannelMessageCommand(
                principal.accountId(),
                request.channelId() == null ? 0L : request.channelId(),
                toDraft(request, messageApplicationService)
        ));
    }

    private team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft toDraft(
            RealtimeClientMessage request,
            MessageApplicationService messageApplicationService
    ) {
        String messageType = requireMessageType(request.messageType());
        if (TEXT_MESSAGE_TYPE.equals(messageType)) {
            return new TextChannelMessageDraft(request.body());
        }
        if (FILE_MESSAGE_TYPE.equals(messageType)) {
            Map<String, Object> payload = requirePayload(request.payload());
            String metadata = request.metadata() == null ? null : jsonProvider.toJson(request.metadata());
            return new FileChannelMessageDraft(
                    request.body(),
                    textValue(payload, "object_key"),
                    textValue(payload, "filename"),
                    textValue(payload, "mime_type"),
                    longValue(payload, "size"),
                    metadata
            );
        }
        if (VOICE_MESSAGE_TYPE.equals(messageType)) {
            Map<String, Object> payload = requirePayload(request.payload());
            String metadata = request.metadata() == null ? null : jsonProvider.toJson(request.metadata());
            return new VoiceChannelMessageDraft(
                    request.body(),
                    textValue(payload, "object_key"),
                    textValue(payload, "filename"),
                    textValue(payload, "mime_type"),
                    longValue(payload, "size"),
                    longValue(payload, "duration_millis"),
                    textValue(payload, "transcript"),
                    metadata
            );
        }
        if (!messageApplicationService.supportsExtensionMessageType(messageType)) {
            throw ProblemException.validationFailed("unsupported extension message type");
        }
        String payload = request.payload() == null ? null : jsonProvider.toJson(request.payload());
        String metadata = request.metadata() == null ? null : jsonProvider.toJson(request.metadata());
        return new PluginChannelMessageDraft(messageType, request.body(), messageType, payload, metadata);
    }

    private String requireMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw ProblemException.validationFailed("messageType must not be blank");
        }
        return messageType.trim();
    }

    private Map<String, Object> requirePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw ProblemException.validationFailed("payload must not be null");
        }
        return payload;
    }

    private String textValue(Map<String, Object> payload, String fieldName) {
        Object field = payload.get(fieldName);
        return field == null ? null : String.valueOf(field);
    }

    private Long longValue(Map<String, Object> payload, String fieldName) {
        Object field = payload.get(fieldName);
        if (field == null) {
            return null;
        }
        if (field instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(field));
    }
}
