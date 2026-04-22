package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 消息应用服务。
 * 职责：编排频道文本消息发送与历史查询用例。
 * 边界：当前阶段不承载文件消息、插件消息或跨服务端分发。
 */
@Service
public class MessageApplicationService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageRealtimePublisher messageRealtimePublisher;
    private final ServerIdentityProperties serverIdentityProperties;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public MessageApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            MessageRepository messageRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.messageRepository = messageRepository;
        this.messageRealtimePublisher = messageRealtimePublisher;
        this.serverIdentityProperties = serverIdentityProperties;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 发送频道文本消息。
     *
     * @param command 发送命令
     * @return 已持久化并已用于实时分发的消息结果
     */
    public ChannelMessageResult sendChannelTextMessage(SendChannelTextMessageCommand command) {
        validateSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            requireMembership(channel.id(), command.accountId());
            ChannelMessage message = new ChannelMessage(
                    idGenerator.nextLongId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    "text",
                    command.content().trim(),
                    null,
                    null,
                    "sent",
                    timeProvider.nowInstant()
            );
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            return new PersistedMessage(savedMessage, recipientAccountIds);
        });
        messageRealtimePublisher.publish(persistedMessage.message(), persistedMessage.recipientAccountIds());
        return toResult(persistedMessage.message());
    }

    /**
     * 查询频道历史消息。
     *
     * @param query 历史查询参数
     * @return 历史消息结果
     */
    public ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query) {
        validateHistoryQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.findByChannelIdBefore(
                        channel.id(),
                        query.cursorMessageId(),
                        query.limit()
                ).stream()
                .map(this::toResult)
                .toList();
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).messageId();
        return new ChannelMessageHistoryResult(messages, nextCursor);
    }

    private void validateSendCommand(SendChannelTextMessageCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (command.content() == null || command.content().isBlank()) {
            throw ProblemException.validationFailed("content must not be blank");
        }
    }

    private void validateHistoryQuery(GetChannelMessageHistoryQuery query) {
        if (query.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (query.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursorMessageId must be greater than 0");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    private void requireMembership(long channelId, long accountId) {
        if (!channelMemberRepository.exists(channelId, accountId)) {
            throw ProblemException.forbidden("channel_member_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
    }

    private ChannelMessageResult toResult(ChannelMessage message) {
        return new ChannelMessageResult(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.content(),
                message.payload(),
                message.metadata(),
                message.status(),
                message.createdAt()
        );
    }

    private record PersistedMessage(ChannelMessage message, List<Long> recipientAccountIds) {
    }
}
