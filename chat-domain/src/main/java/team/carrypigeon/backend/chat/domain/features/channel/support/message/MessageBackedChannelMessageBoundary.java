package team.carrypigeon.backend.chat.domain.features.channel.support.message;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelMessageBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 基于 message feature 的频道消息边界适配器。
 * 职责：把 channel 侧的消息读取语义映射到 message 仓储。
 * 边界：只处理跨 feature 查询适配，不承载频道业务规则。
 */
@Component
public class MessageBackedChannelMessageBoundary implements ChannelMessageBoundary {

    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";

    private final MessageRepository messageRepository;

    public MessageBackedChannelMessageBoundary(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessageSnapshot requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .map(this::toSnapshot)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    @Override
    public boolean hasMessages(long channelId) {
        return !messageRepository.findByChannelIdBefore(channelId, null, 1).isEmpty();
    }

    private ChannelMessageSnapshot toSnapshot(ChannelMessage message) {
        return new ChannelMessageSnapshot(message.messageId(), message.channelId());
    }
}
