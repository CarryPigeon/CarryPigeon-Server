package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.forward;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.MessageHandler;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.CPChannelMessageHandler;

/**
 * 校验转发消息的来源频道，并以十进制字符串保存转发载荷中的 ID。
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
        long sourceChannelId;
        List<Long> messageIds;
        try {
            var forward = objectMapper.convertValue(message.data(), CPChannelForwardMessageData.class);
            if (forward == null || forward.getMessageIds() == null) {
                throw ProblemException.of(MessageProblem.INVALID_INPUT);
            }
            sourceChannelId = Long.parseLong(forward.getSourceChannelId());
            messageIds = forward.getMessageIds().stream().map(Long::valueOf).toList();
        } catch (IllegalArgumentException invalidId) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "forward message IDs must be positive integers within the long range");
        }
        if (sourceChannelId <= 0 || messageIds.stream().anyMatch(id -> id <= 0)) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT);
        }
        // 不允许伪造消息源，其他的保持不变
        for (CPChannelMessage messageItem : messageRepository.findByIds(messageIds)) {
            if (messageItem.channelId() != sourceChannelId) {
                throw ProblemException.of(MessageProblem.INVALID_INPUT);
            }
        }

        // 自定义字段随原载荷保存，已声明的 ID 统一按响应协议输出，以免客户端丢失精度。
        Map<String,Object> data = new LinkedHashMap<>(message.data());
        data.put("source_channel_id", Long.toString(sourceChannelId));
        data.put("message_ids", messageIds.stream().map(String::valueOf).toList());
        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.domain(),
            data,
            message.sendTime(),
            message.mentions(),
            "转发消息",
            "转发消息",
            message.status()
        );
    }
}
