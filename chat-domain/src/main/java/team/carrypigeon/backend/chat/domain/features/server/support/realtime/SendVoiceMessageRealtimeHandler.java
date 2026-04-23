package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 语音消息实时入站处理器。
 * 职责：解析统一 realtime 语音消息命令并委托消息应用服务发送。
 * 边界：只负责协议入站到草稿对象的转换，不承载语音消息业务规则。
 */
public class SendVoiceMessageRealtimeHandler implements RealtimeInboundMessageHandler {

    private static final String GENERIC_SEND_COMMAND = "send_channel_message";

    private final JsonProvider jsonProvider;

    public SendVoiceMessageRealtimeHandler(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public boolean supports(RealtimeClientMessage request) {
        return request != null
                && GENERIC_SEND_COMMAND.equals(request.type())
                && "voice".equals(request.messageType());
    }

    @Override
    public void handle(
            AuthenticatedPrincipal principal,
            RealtimeClientMessage request,
            MessageApplicationService messageApplicationService
    ) {
        Map<String, Object> payload = requirePayload(request.payload());
        String metadata = request.metadata() == null ? null : jsonProvider.toJson(request.metadata());
        messageApplicationService.sendChannelMessage(new SendChannelMessageCommand(
                principal.accountId(),
                request.channelId() == null ? 0L : request.channelId(),
                new VoiceChannelMessageDraft(
                        request.body(),
                        textValue(payload, "object_key"),
                        textValue(payload, "filename"),
                        textValue(payload, "mime_type"),
                        longValue(payload, "size"),
                        longValue(payload, "duration_millis"),
                        textValue(payload, "transcript"),
                        metadata
                )
        ));
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
