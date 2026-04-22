package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;

/**
 * 频道消息实时入站处理器。
 * 职责：兼容当前 legacy text 命令，并为未来基于消息类型的统一发送命令预留扩展入口。
 * 边界：当前阶段只把 text 消息草稿委托给消息应用服务，不直接承载其它类型规则。
 */
public class SendChannelMessageRealtimeHandler implements RealtimeInboundMessageHandler {

    private static final String LEGACY_TEXT_COMMAND = "send_channel_text_message";
    private static final String GENERIC_SEND_COMMAND = "send_channel_message";

    @Override
    public boolean supports(RealtimeClientMessage request) {
        if (request == null || request.type() == null) {
            return false;
        }
        if (LEGACY_TEXT_COMMAND.equals(request.type())) {
            return true;
        }
        return GENERIC_SEND_COMMAND.equals(request.type()) && "text".equals(request.messageType());
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
                new TextChannelMessageDraft(request.body())
        ));
    }
}
