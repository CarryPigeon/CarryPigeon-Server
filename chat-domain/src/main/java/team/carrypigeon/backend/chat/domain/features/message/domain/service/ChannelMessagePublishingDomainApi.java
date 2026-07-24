package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
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
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.MessageDomainPluginApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.command.ValidateMessageDataCommand;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.ValidatedMessageDataResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：创建 canonical 消息，并实现 ReplyText 与单条/合并 Forward。
 * 边界：不承载撤回、查询、附件上传和置顶能力。
 */
@Service
public class ChannelMessagePublishingDomainApi extends AbstractMessageDomainSupport implements ChannelMessagePublishingApi {

    private static final String FORWARD_OPERATION = "message.forward.v1";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final MessageDeliveryCommandValidator commandValidator;
    private final MessageIdempotencyRepository messageIdempotencyRepository;
    private final MessageDomainPluginApi messageDomainPluginApi;

    public ChannelMessagePublishingDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageIdempotencyRepository messageIdempotencyRepository,
            RealtimeEventApi realtimeEventApi,
            MessageDomainPluginApi messageDomainPluginApi,
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
        this.commandValidator = new MessageDeliveryCommandValidator();
        this.messageIdempotencyRepository = messageIdempotencyRepository;
        this.messageDomainPluginApi = messageDomainPluginApi;
    }

    @Override
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        commandValidator.validateSendCommand(command);
        PersistedMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = requireSendableChannel(command.channelId(), command.accountId());
            ChannelMessage message = buildCanonicalMessage(
                    channel,
                    command.accountId(),
                    command.domain(),
                    command.domainVersion(),
                    command.data(),
                    command.mentions(),
                    true
            );
            return persistCreatedMessage(afterCommit, message, channel);
        });
        return toResult(persisted.message());
    }

    @Override
    public ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command) {
        commandValidator.validateSystemSendCommand(command);
        PersistedMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = requireChannel(command.channelId());
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
        return toResult(persisted.message());
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
            ChannelMessagingContext targetChannel = requireSendableChannel(command.targetChannelId(), command.accountId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("domain", CORE_TEXT_DOMAIN);
            data.put("domain_version", CORE_TEXT_DOMAIN_VERSION);
            if (comment != null) {
                data.put("content", Map.of("text", comment));
            }
            if (command.mergedMessageIds() == null || command.mergedMessageIds().isEmpty()) {
                ChannelMessage source = requireMessage(command.sourceMessageId());
                requireMemberChannel(source.channelId(), command.accountId());
                data.put("forwarded_from", forwardSource(source));
            } else {
                List<Map<String, Object>> sources = new ArrayList<>();
                for (Long messageId : command.mergedMessageIds()) {
                    ChannelMessage source = messageRepository.findById(messageId).orElse(null);
                    if (source == null) {
                        sources.add(Map.of("mid", Long.toString(messageId), "unavailable", true));
                        continue;
                    }
                    requireMemberChannel(source.channelId(), command.accountId());
                    sources.add(forwardSource(source));
                }
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
            PersistedMessage persisted = persistCreatedMessage(afterCommit, message, targetChannel);
            if (reservation != null) {
                messageIdempotencyRepository.complete(
                        command.accountId(),
                        FORWARD_OPERATION,
                        idempotencyKey,
                        requestFingerprint,
                        persisted.message().messageId(),
                        now()
                );
            }
            return persisted.message();
        });
        return toResult(result);
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

    private PersistedMessage persistCreatedMessage(
            TransactionRunner.AfterCommitExecutor afterCommit,
            ChannelMessage message,
            ChannelMessagingContext channel
    ) {
        List<Long> recipients = channelMessagingApi.recipientAccountIds(channel.id());
        ChannelMessage saved = messageRepository.save(message);
        List<Mention> mentions = messageMentionManager.persistMentions(saved, recipients);
        PersistedMessage persisted = new PersistedMessage(saved, recipients, mentions);
        messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, persisted);
        return persisted;
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

}
