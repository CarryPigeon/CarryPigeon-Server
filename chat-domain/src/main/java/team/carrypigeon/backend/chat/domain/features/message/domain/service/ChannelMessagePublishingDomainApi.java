package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagePolicyApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.MessageDomainPluginApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.command.ValidateMessageDataCommand;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.ValidatedMessageDataResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：创建 canonical 消息，并实现 ReplyText 与单条/合并 Forward。
 * 边界：不承载撤回、查询、附件上传和置顶能力。
 */
@Service
public class ChannelMessagePublishingDomainApi implements ChannelMessagePublishingApi {

    private static final String CORE_TEXT_DOMAIN = "Core:Text";
    private static final String CORE_FORWARD_DOMAIN = "Core:Forward";
    private static final String CORE_TEXT_DOMAIN_VERSION = "1.0.0";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String FORWARD_OPERATION = "message.forward.v1";
    private static final String SEND_OPERATION = "message.send.v1";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final MessageDeliveryCommandValidator commandValidator;
    private final ChannelContextApi channelContextApi;
    private final ChannelMessagePolicyApi channelMessagePolicyApi;
    private final MessageRepository messageRepository;
    private final MessageMentionManager messageMentionManager;
    private final MessageAfterCommitPublisher messageAfterCommitPublisher;
    private final MessageIdempotencyRepository messageIdempotencyRepository;
    private final MessageDomainPluginApi messageDomainPluginApi;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelMessagePublishingDomainApi(
            ChannelContextApi channelContextApi,
            ChannelMessagePolicyApi channelMessagePolicyApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageIdempotencyRepository messageIdempotencyRepository,
            ApplicationEventPublisher eventPublisher,
            MessageDomainPluginApi messageDomainPluginApi,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelContextApi = channelContextApi;
        this.channelMessagePolicyApi = channelMessagePolicyApi;
        this.messageRepository = messageRepository;
        this.messageMentionManager = new MessageMentionManager(mentionRepository, idGenerator, timeProvider);
        this.messageAfterCommitPublisher = new MessageAfterCommitPublisher(eventPublisher, timeProvider);
        this.commandValidator = new MessageDeliveryCommandValidator();
        this.messageIdempotencyRepository = messageIdempotencyRepository;
        this.messageDomainPluginApi = messageDomainPluginApi;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        commandValidator.validateSendCommand(command);
        String idempotencyKey = normalizedIdempotencyKey(command.clientMessageId());
        if (idempotencyKey != null && idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw ProblemException.validationFailed("client_message_id length must be less than or equal to 128");
        }
        String requestFingerprint = idempotencyKey == null ? null : sendRequestFingerprint(command);
        ChannelMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            MessageIdempotency reservation = reserveSendIdempotency(command, idempotencyKey, requestFingerprint);
            if (reservation != null && reservation.messageId() != null) {
                return messageRepository.findById(reservation.messageId())
                        .orElseThrow(() -> ProblemException.fail(
                                "idempotency_result_missing",
                                "idempotency result message is unavailable"
                        ));
            }
            ChannelMessagingContext channel = channelMessagePolicyApi.requireSendableChannel(
                    command.channelId(), command.accountId(), now()
            );
            ChannelMessage message = buildCanonicalMessage(
                    channel,
                    command.accountId(),
                    command.domain(),
                    command.domainVersion(),
                    command.data(),
                    command.mentions(),
                    true
            );
            ChannelMessage created = persistCreatedMessage(afterCommit, message, channel);
            if (reservation != null) {
                messageIdempotencyRepository.complete(
                        command.accountId(),
                        SEND_OPERATION,
                        idempotencyKey,
                        requestFingerprint,
                        created.messageId(),
                        now()
                );
            }
            return created;
        });
        return ChannelMessageProjectionMapper.toResult(persisted);
    }

    private MessageIdempotency reserveSendIdempotency(
            SendChannelMessageCommand command,
            String idempotencyKey,
            String requestFingerprint
    ) {
        if (idempotencyKey == null) {
            return null;
        }
        MessageIdempotency reservation = messageIdempotencyRepository.reserve(new MessageIdempotency(
                command.accountId(),
                SEND_OPERATION,
                idempotencyKey,
                requestFingerprint,
                null,
                now(),
                null
        ));
        if (!requestFingerprint.equals(reservation.requestFingerprint())) {
            throw ProblemException.conflict(
                    "idempotency_key_reused",
                    "client_message_id has already been used for a different request"
            );
        }
        return reservation;
    }

    @Override
    public ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command) {
        commandValidator.validateSystemSendCommand(command);
        ChannelMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = channelContextApi.requireChannel(command.channelId());
            requireSystemChannel(channel);
            ChannelMessage message = buildCanonicalMessage(
                    channel,
                    command.operatorAccountId(),
                    "Core:System",
                    command.domainVersion(),
                    command.data(),
                    command.mentions(),
                    false
            );
            return persistCreatedMessage(afterCommit, message, channel);
        });
        return ChannelMessageProjectionMapper.toResult(persisted);
    }

    @Override
    public ChannelMessageResult forwardChannelMessage(ForwardChannelMessageCommand command) {
        validateForwardCommand(command);
        String idempotencyKey = normalizedIdempotencyKey(command.idempotencyKey());
        String comment = normalizedComment(command.comment());
        String requestFingerprint = idempotencyKey == null ? null : forwardRequestFingerprint(command, comment);
        ChannelMessage result = transactionRunner.runInTransaction(afterCommit -> {
            MessageIdempotency reservation = reserveForwardIdempotency(
                    command,
                    idempotencyKey,
                    requestFingerprint
            );
            if (reservation != null && reservation.messageId() != null) {
                return messageRepository.findById(reservation.messageId())
                        .orElseThrow(() -> ProblemException.fail(
                                "idempotency_result_missing",
                                "idempotency result message is unavailable"
                        ));
            }
            ChannelMessagingContext targetChannel = channelMessagePolicyApi.requireSendableChannel(
                    command.targetChannelId(), command.accountId(), now()
            );
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("domain", CORE_TEXT_DOMAIN);
            data.put("domain_version", CORE_TEXT_DOMAIN_VERSION);
            if (comment != null) {
                data.put("content", Map.of("text", comment));
            }
            if (command.mergedMessageIds() == null || command.mergedMessageIds().isEmpty()) {
                ChannelMessage source = requireMessage(command.sourceMessageId());
                channelContextApi.requireMemberChannel(source.channelId(), command.accountId());
                data.put("forwarded_from", forwardSource(source));
            } else {
                Map<Long, ChannelMessage> messagesById = messageRepository.findByIds(command.mergedMessageIds());
                LinkedHashSet<Long> sourceChannelIds = new LinkedHashSet<>();
                List<Map<String, Object>> sources = new ArrayList<>();
                for (Long messageId : command.mergedMessageIds()) {
                    ChannelMessage source = messagesById.get(messageId);
                    if (source == null) {
                        sources.add(Map.of("mid", Long.toString(messageId), "unavailable", true));
                        continue;
                    }
                    sourceChannelIds.add(source.channelId());
                    sources.add(forwardSource(source));
                }
                sourceChannelIds.forEach(channelId ->
                        channelContextApi.requireMemberChannel(channelId, command.accountId()));
                data.put("forwarded_messages", List.copyOf(sources));
            }
            ChannelMessage message = buildCanonicalMessage(
                    targetChannel,
                    command.accountId(),
                    CORE_FORWARD_DOMAIN,
                    CORE_TEXT_DOMAIN_VERSION,
                    data,
                    List.of(),
                    false
            );
            ChannelMessage persisted = persistCreatedMessage(afterCommit, message, targetChannel);
            if (reservation != null) {
                messageIdempotencyRepository.complete(
                        command.accountId(),
                        FORWARD_OPERATION,
                        idempotencyKey,
                        requestFingerprint,
                        persisted.messageId(),
                        now()
                );
            }
            return persisted;
        });
        return ChannelMessageProjectionMapper.toResult(result);
    }

    private MessageIdempotency reserveForwardIdempotency(
            ForwardChannelMessageCommand command,
            String idempotencyKey,
            String requestFingerprint
    ) {
        if (idempotencyKey == null) {
            return null;
        }
        MessageIdempotency reservation = messageIdempotencyRepository.reserve(new MessageIdempotency(
                command.accountId(),
                FORWARD_OPERATION,
                idempotencyKey,
                requestFingerprint,
                null,
                now(),
                null
        ));
        if (!requestFingerprint.equals(reservation.requestFingerprint())) {
            throw ProblemException.conflict(
                    "idempotency_key_reused",
                    "idempotency key has already been used for a different request"
            );
        }
        return reservation;
    }

    private ChannelMessage buildCanonicalMessage(
            ChannelMessagingContext channel,
            long senderId,
            String domain,
            String domainVersion,
            Map<String, Object> data,
            List<Long> mentions,
            boolean clientRequest
    ) {
        long messageId = nextMessageId();
        java.time.Instant sendTime = now();
        ValidatedMessageDataResult content = messageDomainPluginApi.validateMessageData(new ValidateMessageDataCommand(
                messageId,
                channel.id(),
                senderId,
                sendTime,
                domain,
                domainVersion,
                data,
                clientRequest
        ));
        return new ChannelMessage(
                messageId,
                senderId,
                channel.id(),
                content.domain(),
                content.domainVersion(),
                content.data(),
                sendTime,
                messageMentionManager.normalizeMentions(mentions),
                content.preview(),
                MessageStatus.SENT
        );
    }

    private ChannelMessage persistCreatedMessage(
            TransactionRunner.AfterCommitExecutor afterCommit,
            ChannelMessage message,
            ChannelMessagingContext channel
    ) {
        List<Long> recipients = channelContextApi.recipientAccountIds(channel.id());
        ChannelMessage saved = messageRepository.save(message);
        List<Mention> mentions = messageMentionManager.persistMentions(saved, recipients);
        messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, saved, recipients, mentions);
        return saved;
    }

    private void validateForwardCommand(ForwardChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.sourceMessageId(), "sourceMessageId");
        requirePositive(command.targetChannelId(), "targetChannelId");
        if (command.comment() != null && command.comment().trim().length() > 500) {
            throw ProblemException.validationFailed("comment length must be less than or equal to 500");
        }
        String idempotencyKey = normalizedIdempotencyKey(command.idempotencyKey());
        if (idempotencyKey != null && idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw ProblemException.validationFailed("idempotency key length must be less than or equal to 128");
        }
        if (command.mergedMessageIds() != null && !command.mergedMessageIds().isEmpty()) {
            if (command.mergedMessageIds().size() < 2) {
                throw ProblemException.validationFailed("merged_mids must contain at least two ids");
            }
            for (Long messageId : command.mergedMessageIds()) {
                if (messageId == null || messageId <= 0L) {
                    throw ProblemException.validationFailed("merged_mids must contain positive snowflake ids");
                }
            }
        }
    }

    private String normalizedIdempotencyKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    private String forwardRequestFingerprint(ForwardChannelMessageCommand command, String normalizedComment) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateFingerprint(digest, FORWARD_OPERATION);
            updateFingerprint(digest, command.sourceMessageId());
            updateFingerprint(digest, command.targetChannelId());
            updateFingerprint(digest, normalizedComment);
            List<Long> mergedMessageIds = command.mergedMessageIds() == null ? List.of() : command.mergedMessageIds();
            updateFingerprint(digest, mergedMessageIds.size());
            for (Long messageId : mergedMessageIds) {
                updateFingerprint(digest, messageId);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String sendRequestFingerprint(SendChannelMessageCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateFingerprint(digest, SEND_OPERATION);
            updateFingerprint(digest, command.channelId());
            updateFingerprint(digest, command.domain());
            updateFingerprint(digest, command.domainVersion());
            updateCanonicalValue(digest, command.data());
            updateCanonicalValue(digest, command.mentions());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void updateCanonicalValue(MessageDigest digest, Object value) {
        if (value == null) {
            updateFingerprint(digest, "null");
        } else if (value instanceof Map<?, ?> map) {
            updateFingerprint(digest, "map");
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> {
                        updateFingerprint(digest, String.valueOf(entry.getKey()));
                        updateCanonicalValue(digest, entry.getValue());
                    });
        } else if (value instanceof List<?> list) {
            updateFingerprint(digest, "list");
            list.forEach(item -> updateCanonicalValue(digest, item));
        } else {
            updateFingerprint(digest, value.getClass().getName());
            updateFingerprint(digest, String.valueOf(value));
        }
    }

    private void updateFingerprint(MessageDigest digest, long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    private void updateFingerprint(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private String normalizedComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private void requireSystemChannel(ChannelMessagingContext channel) {
        if (!"system".equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_required", "system message requires system channel");
        }
    }

    private Map<String, Object> forwardSource(ChannelMessage sourceMessage) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("mid", Long.toString(sourceMessage.messageId()));
        source.put("cid", Long.toString(sourceMessage.channelId()));
        source.put("uid", Long.toString(sourceMessage.senderId()));
        source.put("preview", sourceMessage.preview());
        source.put("send_time", sourceMessage.sendTime().toEpochMilli());
        return source;
    }

    private long nextMessageId() {
        return idGenerator.nextLongId();
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
