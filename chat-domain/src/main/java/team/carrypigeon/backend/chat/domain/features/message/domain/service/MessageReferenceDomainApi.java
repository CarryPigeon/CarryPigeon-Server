package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息引用查询 API 实现。
 * 职责：在 message feature 内完成消息引用和频道消息存在性查询。
 * 边界：不向调用方暴露消息仓储或完整聚合。
 */
@Service
public class MessageReferenceDomainApi implements MessageReferenceApi {

    private final MessageRepository messageRepository;

    public MessageReferenceDomainApi(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public MessageReferenceResult requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .map(message -> new MessageReferenceResult(message.messageId(), message.channelId()))
                .orElseThrow(() -> ProblemException.notFound("message does not exist"));
    }

    @Override
    public boolean hasChannelMessages(long channelId) {
        return !messageRepository.findByChannelIdBefore(channelId, null, 1).isEmpty();
    }
}
