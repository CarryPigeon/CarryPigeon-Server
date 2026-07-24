package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelPinApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道置顶领域 API 实现。
 * 职责：直接承载频道消息置顶、取消置顶和置顶列表用例实现。
 * 边界：不暴露普通消息发送、编辑和历史搜索能力。
 */
@Service
public class ChannelPinDomainApi extends AbstractMessageDomainSupport implements ChannelPinApi {

    public ChannelPinDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        super(
                channelMessagingApi,
                messageRepository,
                mentionRepository,
                realtimeEventApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    @Override
    public ChannelPinResult pinChannelMessage(PinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        if (command.note() != null && command.note().trim().length() > 200) {
            throw ProblemException.validationFailed("note length must be less than or equal to 200");
        }
        PinnedChannelMessage pinnedChannelMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = requireChannel(command.channelId());
            channelMessagingApi.requirePinModerationPermission(channel.id(), command.accountId());
            ChannelMessage message = requireMessage(command.messageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            if (channelMessagingApi.findPin(channel.id(), message.messageId()).isEmpty()
                    && channelMessagingApi.countPins(channel.id()) >= MAX_PINS_PER_CHANNEL) {
                throw ProblemException.validationFailed("pin_limit_reached", "channel pin limit is reached");
            }
            ChannelPinReference pin = channelMessagingApi.savePin(
                    idGenerator.nextLongId(),
                    channel.id(),
                    message.messageId(),
                    command.accountId(),
                    command.note() == null ? "" : command.note().trim(),
                    now()
            );
            PinnedChannelMessage pinnedResult = new PinnedChannelMessage(pin, channelMessagingApi.recipientAccountIds(channel.id()));
            messageAfterCommitPublisher.publishMessagePinnedAfterCommit(afterCommit, pinnedResult);
            return pinnedResult;
        });
        return toPinResult(pinnedChannelMessage.pin());
    }

    @Override
    public void unpinChannelMessage(UnpinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = requireChannel(command.channelId());
            channelMessagingApi.requirePinModerationPermission(channel.id(), command.accountId());
            ChannelPinReference pin = channelMessagingApi.findPin(channel.id(), command.messageId())
                    .orElseThrow(() -> ProblemException.notFound("channel pin does not exist"));
            channelMessagingApi.deletePin(channel.id(), command.messageId());
            UnpinnedChannelMessage unpinnedChannelMessage = new UnpinnedChannelMessage(
                    pin,
                    command.accountId(),
                    now().toEpochMilli(),
                    channelMessagingApi.recipientAccountIds(channel.id())
            );
            messageAfterCommitPublisher.publishMessageUnpinnedAfterCommit(afterCommit, unpinnedChannelMessage);
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
        ChannelMessagingContext channel = channelMessagingApi.requireMemberChannel(query.channelId(), query.accountId());
        return channelMessagingApi.findPinsBefore(channel.id(), query.cursorMessageId(), query.limit() + 1).stream()
                .map(this::toPinResult)
                .toList();
    }
}
