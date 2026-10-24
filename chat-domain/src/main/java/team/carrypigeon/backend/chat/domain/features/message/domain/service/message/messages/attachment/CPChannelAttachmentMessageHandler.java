package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.MessageHandler;

/**
 * CarryPigeon 预定义的附件消息处理器
 * 职责：处理附件消息，校验其格式正确
 * 边界：不关心消息的生命周期，只关注消息的格式
 * */
@MessageHandler("Core:Attachment:V1")
public class CPChannelAttachmentMessageHandler implements CPChannelMessageHandler {
    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        // TODO 完善handler
        return null;
    }
}
