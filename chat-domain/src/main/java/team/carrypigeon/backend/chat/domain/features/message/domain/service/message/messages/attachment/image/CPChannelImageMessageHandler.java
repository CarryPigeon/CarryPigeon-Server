package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment.image;

import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment.CPChannelAttachmentMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.annotation.MessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;

/**
 * 图片消息处理器，直接复用附件消息处理器，客户端将文件处理成图片即可
 */
@MessageHandler(CPChannelImageMessageHandler.DOMAIN)
public class CPChannelImageMessageHandler implements CPChannelMessageHandler {
    public final static String DOMAIN = "Core:Image:V1";
    private final CPChannelAttachmentMessageHandler cpChannelAttachmentMessageHandler;

    public CPChannelImageMessageHandler(CPChannelAttachmentMessageHandler cpChannelAttachmentMessageHandler) {
        this.cpChannelAttachmentMessageHandler = cpChannelAttachmentMessageHandler;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        return cpChannelAttachmentMessageHandler.handle(message);
    }
}
