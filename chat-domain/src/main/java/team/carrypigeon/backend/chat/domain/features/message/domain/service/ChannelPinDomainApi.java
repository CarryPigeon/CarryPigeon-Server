package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelPinApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelPinManagementApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RemoveChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.SetChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelPinReferencesQuery;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道置顶领域 API 实现。
 * 职责：直接承载频道消息置顶、取消置顶和置顶列表用例实现。
 * 边界：不暴露普通消息发送、编辑和历史搜索能力。
 */
@Service
public class ChannelPinDomainApi implements ChannelPinApi {

    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private final ChannelContextApi channelContextApi;
    private final ChannelPinManagementApi channelPinManagementApi;
    private final MessageRepository messageRepository;
    private final MessageAfterCommitPublisher messageAfterCommitPublisher;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelPinDomainApi(
            ChannelContextApi channelContextApi,
            ChannelPinManagementApi channelPinManagementApi,
            MessageRepository messageRepository,
            ApplicationEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelContextApi = channelContextApi;
        this.channelPinManagementApi = channelPinManagementApi;
        this.messageRepository = messageRepository;
        this.messageAfterCommitPublisher = new MessageAfterCommitPublisher(eventPublisher, timeProvider);
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ChannelPinResult pinChannelMessage(PinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        ChannelPinReference pin = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = channelContextApi.requireChannel(command.channelId());
            ChannelMessage message = requireMessage(command.messageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            ChannelPinReference savedPin = channelPinManagementApi.setPin(new SetChannelPinCommand(
                    idGenerator.nextLongId(),
                    channel.id(),
                    message.messageId(),
                    command.accountId(),
                    command.note(),
                    now()
            ));
            messageAfterCommitPublisher.publishMessagePinnedAfterCommit(
                    afterCommit,
                    savedPin,
                    channelContextApi.recipientAccountIds(channel.id())
            );
            return savedPin;
        });
        return toPinResult(pin);
    }

    @Override
    public void unpinChannelMessage(UnpinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            ChannelPinReference pin = channelPinManagementApi.removePin(new RemoveChannelPinCommand(
                    command.channelId(), command.messageId(), command.accountId()
            ));
            messageAfterCommitPublisher.publishMessageUnpinnedAfterCommit(
                    afterCommit,
                    pin,
                    command.accountId(),
                    now().toEpochMilli(),
                    channelContextApi.recipientAccountIds(pin.channelId())
            );
        });
    }

    @Override
    public List<ChannelPinResult> listChannelPins(ListChannelPinsQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
        return channelPinManagementApi.listPins(new ListChannelPinReferencesQuery(
                        query.accountId(), query.channelId(), query.cursorMessageId(), query.limit() + 1
                )).stream()
                .map(this::toPinResult)
                .toList();
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private ChannelPinResult toPinResult(ChannelPinReference pin) {
        return new ChannelPinResult(
                pin.pinId(),
                pin.channelId(),
                pin.messageId(),
                pin.pinnedByAccountId(),
                pin.pinnedAt(),
                pin.note()
        );
    }

    private java.time.Instant now() {
        return timeProvider.nowInstant();
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
