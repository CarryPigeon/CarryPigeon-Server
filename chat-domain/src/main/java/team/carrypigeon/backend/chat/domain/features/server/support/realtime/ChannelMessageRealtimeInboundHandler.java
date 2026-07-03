package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.Map;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.CustomChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 频道消息 realtime 入站处理器。
 * 职责：承接当前保留的 WebSocket 发消息兼容命令，并转换为 message 领域服务命令。
 * 边界：这里只做协议字段校验与草稿映射，不承担频道鉴权、持久化或广播规则。
 */
@Component
public class ChannelMessageRealtimeInboundHandler implements RealtimeInboundMessageHandler {

    private final JsonProvider jsonProvider;

    public ChannelMessageRealtimeInboundHandler(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public boolean supports(RealtimeClientMessage request) {
        return "send_channel_message".equals(request.type());
    }

    @Override
    public void handle(
            AuthenticatedAccount principal,
            RealtimeClientMessage request,
            ChannelMessagePublishingApi channelMessagePublishingApi
    ) {
        long channelId = requirePositiveLong(request.channelId(), "channel_id");
        String messageType = requireNonBlank(request.messageType(), "message_type must not be blank");
        ChannelMessageDraft draft = toDraft(messageType, request);
        channelMessagePublishingApi.sendChannelMessage(new SendChannelMessageCommand(
                principal.accountId(),
                channelId,
                draft
        ));
    }

    private ChannelMessageDraft toDraft(String messageType, RealtimeClientMessage request) {
        Map<String, Object> payload = request.payload();
        String metadata = toJson(request.metadata());
        return switch (messageType) {
            case "text" -> new TextChannelMessageDraft(request.body());
            case "file" -> new FileChannelMessageDraft(
                    request.body(),
                    requiredText(payload, "object_key"),
                    requiredText(payload, "filename"),
                    optionalText(payload, "mime_type"),
                    optionalLong(payload, "size"),
                    metadata
            );
            case "voice" -> new VoiceChannelMessageDraft(
                    request.body(),
                    requiredText(payload, "object_key"),
                    requiredText(payload, "filename"),
                    optionalText(payload, "mime_type"),
                    optionalLong(payload, "size"),
                    requirePositiveLong(optionalLong(payload, "duration_millis"), "duration_millis"),
                    optionalText(payload, "transcript"),
                    metadata
            );
            case "custom" -> new CustomChannelMessageDraft(
                    request.body(),
                    toJson(payload),
                    metadata
            );
            case "system" -> throw ProblemException.forbidden(
                    "system_channel_required",
                    "system message is not supported over realtime command"
            );
            default -> new PluginChannelMessageDraft(
                    messageType,
                    request.body(),
                    messageType,
                    toJson(payload),
                    metadata
            );
        };
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return jsonProvider.toJson(value);
    }

    private String requiredText(Map<String, Object> payload, String fieldName) {
        String value = optionalText(payload, fieldName);
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed(fieldName + " must not be blank");
        }
        return value;
    }

    private String optionalText(Map<String, Object> payload, String fieldName) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long optionalLong(Map<String, Object> payload, String fieldName) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed(fieldName + " must be a number");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed(message);
        }
        return value.trim();
    }

    private long requirePositiveLong(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
        return value;
    }
}
