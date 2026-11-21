package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.forward;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.annotation.MessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;

/**
 * 转发消息处理器占位，尚未实现载荷校验与转换，不参与消息能力注册。
 * 实现完整发送契约后才能添加 MessageHandler 注解。
 */
@MessageHandler(CPChannelForwardMessageHandler.DOMAIN)
public class CPChannelForwardMessageHandler implements CPChannelMessageHandler {

    public static final String DOMAIN = "Core:Forward:V1";
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public CPChannelForwardMessageHandler(MessageRepository messageRepository, ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        // 消息校验
        CPChannelForwardMessageData cpChannelForwardMessageData = objectMapper.convertValue(message.data(), CPChannelForwardMessageData.class);
        if (cpChannelForwardMessageData == null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT);
        }
        // 不允许伪造消息源，其他的保持不变
        for (CPChannelMessage messageItem : messageRepository.findByIds(cpChannelForwardMessageData.getMessageIds())) {
            if (messageItem.channelId() != message.channelId()){
                throw ProblemException.of(MessageProblem.INVALID_INPUT);
            }
        }

        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.sequence(),
            message.domain(),
            message.data(),
            message.sendTime(),
            message.mentions(),
            "转发消息",
            "转发消息",
            message.status()
        );
    }
}
