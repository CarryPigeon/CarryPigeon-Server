package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * message 命令侧领域 API 共享支撑。
 * 职责：收敛 canonical 消息仓储、结果投影、提醒索引、事件发布和基础校验。
 * 边界：只供 message feature 内部复用，不作为跨 feature 入口。
 */
abstract class AbstractMessageDomainSupport {

    protected static final String CORE_TEXT_DOMAIN = "Core:Text";
    protected static final String CORE_FILE_DOMAIN = "Core:File";
    protected static final String CORE_VOICE_DOMAIN = "Core:Voice";
    protected static final String CORE_REPLY_TEXT_DOMAIN = "Core:ReplyText";
    protected static final String CORE_FORWARD_DOMAIN = "Core:Forward";
    protected static final String CORE_SYSTEM_DOMAIN = "Core:System";
    protected static final String CORE_TEXT_DOMAIN_VERSION = "1.0.0";
    protected static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    protected static final long MAX_PINS_PER_CHANNEL = 50L;
    protected static final String RECALLED_MESSAGE_PLACEHOLDER = "消息已撤回";

    protected final ChannelMessagingApi channelMessagingApi;
    protected final MessageRepository messageRepository;
    protected final MessageMentionManager messageMentionManager;
    protected final MessageAfterCommitPublisher messageAfterCommitPublisher;
    protected final IdGenerator idGenerator;
    protected final TimeProvider timeProvider;
    protected final TransactionRunner transactionRunner;

    protected AbstractMessageDomainSupport(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelMessagingApi = channelMessagingApi;
        this.messageRepository = messageRepository;
        this.messageMentionManager = new MessageMentionManager(mentionRepository, idGenerator, timeProvider);
        this.messageAfterCommitPublisher = new MessageAfterCommitPublisher(realtimeEventApi, timeProvider);
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    protected ChannelMessage withMentions(ChannelMessage message, List<Long> mentions) {
        return new ChannelMessage(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                message.data(),
                message.sendTime(),
                messageMentionManager.normalizeMentions(mentions),
                message.preview(),
                message.status()
        );
    }

    protected ChannelMessagingContext requireChannel(long channelId) {
        return channelMessagingApi.requireChannel(channelId);
    }

    protected ChannelMessagingContext requireMemberChannel(long channelId, long accountId) {
        return channelMessagingApi.requireMemberChannel(channelId, accountId);
    }

    protected ChannelMessagingContext requireSendableChannel(long channelId, long accountId) {
        return channelMessagingApi.requireSendableChannel(channelId, accountId, now());
    }

    protected void requireSystemChannel(ChannelMessagingContext channel) {
        if (!"system".equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_required", "system message requires system channel");
        }
    }

    protected ChannelMessageResult toResult(ChannelMessage message) {
        return new ChannelMessageResult(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                message.data(),
                message.sendTime(),
                message.mentions(),
                message.preview(),
                message.status()
        );
    }

    protected ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    protected ChannelPinResult toPinResult(ChannelPinReference pin) {
        return new ChannelPinResult(pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.pinnedAt(), pin.note());
    }

    protected ChannelMessage toRecalledMessage(ChannelMessage message) {
        return new ChannelMessage(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                Map.of(),
                message.sendTime(),
                List.of(),
                RECALLED_MESSAGE_PLACEHOLDER,
                MessageStatus.RECALLED
        );
    }

    protected void appendRecallAudit(long channelId, long actorAccountId, long messageId, long senderId) {
        channelMessagingApi.appendMessageRecallAudit(new AppendMessageRecallAuditCommand(
                nextMessageId(), channelId, actorAccountId, messageId, senderId, now()
        ));
    }

    protected boolean isRecalled(ChannelMessage message) {
        return message.status() == MessageStatus.RECALLED;
    }

    protected Map<String, Object> forwardSource(ChannelMessage sourceMessage) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("mid", Long.toString(sourceMessage.messageId()));
        source.put("cid", Long.toString(sourceMessage.channelId()));
        source.put("uid", Long.toString(sourceMessage.senderId()));
        source.put("preview", sourceMessage.preview());
        source.put("send_time", sourceMessage.sendTime().toEpochMilli());
        return source;
    }

    protected long nextMessageId() {
        return idGenerator.nextLongId();
    }

    protected Instant now() {
        return timeProvider.nowInstant();
    }

    protected void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    protected record PersistedMessage(
            ChannelMessage message,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
    }

    protected record PinnedChannelMessage(ChannelPinReference pin, List<Long> recipientAccountIds) {
    }

    protected record UnpinnedChannelMessage(
            ChannelPinReference pin,
            long unpinnedByAccountId,
            long unpinnedAt,
            List<Long> recipientAccountIds
    ) {
    }
}
