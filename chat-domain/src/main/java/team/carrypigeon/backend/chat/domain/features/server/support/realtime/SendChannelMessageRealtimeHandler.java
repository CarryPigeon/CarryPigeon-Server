package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.CustomChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 频道消息实时入站处理器。
 * 职责：兼容当前 legacy text 命令，并为未来基于消息类型的统一发送命令预留扩展入口。
 * 边界：当前阶段只把 text 消息草稿委托给消息应用服务，不直接承载其它类型规则。
 */
public class SendChannelMessageRealtimeHandler implements RealtimeInboundMessageHandler {

    private static final String LEGACY_TEXT_COMMAND = "send_channel_text_message";
    private static final String GENERIC_SEND_COMMAND = "send_channel_message";
    private static final String PLUGIN_KEY = "plugin_key";

    private final JsonProvider jsonProvider;

    public SendChannelMessageRealtimeHandler(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public boolean supports(RealtimeClientMessage request) {
        if (request == null || request.type() == null) {
            return false;
        }
        if (LEGACY_TEXT_COMMAND.equals(request.type())) {
            return true;
        }
        if (!GENERIC_SEND_COMMAND.equals(request.type()) || request.messageType() == null) {
            return false;
        }
        return "text".equals(request.messageType())
                || "plugin".equals(request.messageType())
                || "custom".equals(request.messageType());
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
                toDraft(request)
        ));
    }

    private team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft toDraft(RealtimeClientMessage request) {
        if (LEGACY_TEXT_COMMAND.equals(request.type()) || "text".equals(request.messageType())) {
            return new TextChannelMessageDraft(request.body());
        }
        String payload = request.payload() == null ? null : jsonProvider.toJson(request.payload());
        String metadata = request.metadata() == null ? null : jsonProvider.toJson(request.metadata());
        if ("plugin".equals(request.messageType())) {
            return new PluginChannelMessageDraft(
                    request.body(),
                    requirePluginKey(request.payload()),
                    payload,
                    metadata
            );
        }
        if ("custom".equals(request.messageType())) {
            return new CustomChannelMessageDraft(request.body(), payload, metadata);
        }
        throw ProblemException.validationFailed("unsupported realtime message type");
    }

    private String requirePluginKey(java.util.Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw ProblemException.validationFailed("payload must not be null");
        }
        Object pluginKey = payload.get(PLUGIN_KEY);
        if (pluginKey == null || String.valueOf(pluginKey).isBlank()) {
            throw ProblemException.validationFailed("plugin_key must not be blank");
        }
        return String.valueOf(pluginKey).trim();
    }
}
