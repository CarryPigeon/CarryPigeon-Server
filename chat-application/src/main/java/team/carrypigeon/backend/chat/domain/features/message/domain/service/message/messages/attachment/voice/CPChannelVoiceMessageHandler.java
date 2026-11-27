package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment.voice;

import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment.CPChannelAttachmentMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.MessageHandler;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.CPChannelMessageHandler;

/**
 * 语音消息处理器,直接复用附件处理器，客户端按不同的方式处理文件即可
 */
@MessageHandler(CPChannelVoiceMessageHandler.DOMAIN)
public class CPChannelVoiceMessageHandler implements CPChannelMessageHandler {
    public static final String DOMAIN = "Core:Voice:V1";

    private final CPChannelAttachmentMessageHandler attachmentMessageHandler;

    public CPChannelVoiceMessageHandler(CPChannelAttachmentMessageHandler attachmentMessageHandler) {
        this.attachmentMessageHandler = attachmentMessageHandler;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        return attachmentMessageHandler.handle(message);
    }
}
