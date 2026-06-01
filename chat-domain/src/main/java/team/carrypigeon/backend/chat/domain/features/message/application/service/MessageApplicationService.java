package team.carrypigeon.backend.chat.domain.features.message.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageHttpCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.file.support.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息应用服务。
 * 职责：编排频道消息发送、历史查询与搜索用例。
 * 边界：应用层只编排消息规则和稳定存储抽象，不直接依赖对象存储实现。
 */
@Service
public class MessageApplicationService {

    private static final String SYSTEM_MESSAGE_TYPE = "system";
    private static final String FILE_MESSAGE_TYPE = "file";
    private static final String VOICE_MESSAGE_TYPE = "voice";
    private static final String TEXT_MESSAGE_TYPE = "text";
    private static final String CORE_TEXT_DOMAIN = "Core:Text";
    private static final String CORE_TEXT_DOMAIN_VERSION = "1.0.0";
    private static final String RECALLED_STATUS = "recalled";
    private static final String MESSAGE_RECALLED_AUDIT_ACTION = "MESSAGE_RECALLED";
    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final long MAX_PINS_PER_CHANNEL = 50L;
    private static final long MESSAGE_EDIT_WINDOW_SECONDS = 300L;
    private static final String RECALLED_MESSAGE_PLACEHOLDER = "[消息已撤回]";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelPinRepository channelPinRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final UserProfileRepository userProfileRepository;
    private final MessageRealtimePublisher messageRealtimePublisher;
    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    private final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;
    private final MessageAttachmentPayloadResolver messageAttachmentPayloadResolver;
    private final ServerIdentityProperties serverIdentityProperties;
    private final IdGenerator idGenerator;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    public MessageApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelPinRepository = channelPinRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.userProfileRepository = userProfileRepository;
        this.messageRealtimePublisher = messageRealtimePublisher;
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.serverIdentityProperties = serverIdentityProperties;
        this.idGenerator = idGenerator;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    public MessageApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentObjectKeyPolicy,
                messageAttachmentPayloadResolver,
                serverIdentityProperties,
                idGenerator,
                new JsonProvider(new ObjectMapper().findAndRegisterModules()),
                timeProvider,
                transactionRunner,
                objectStorageServiceProvider
        );
    }

    /**
     * 发送通用频道消息。
     *
     * @param command 发送命令
     * @return 已持久化并已用于实时分发的消息结果
     */
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        validateSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(command.draft().type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), command.draft());
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 判断扩展消息类型是否已在当前运行时注册并允许发送。
     *
     * @param messageType 扩展消息类型
     * @return 允许时返回 true
     */
    public boolean supportsExtensionMessageType(String messageType) {
        return channelMessagePluginRegistry.supportsExtensionMessageType(messageType);
    }

    /**
     * 发送频道文本消息。
     *
     * @param command 发送命令
     * @return 已持久化并已用于实时分发的消息结果
     */
    public ChannelMessageResult sendChannelTextMessage(SendChannelTextMessageCommand command) {
        return sendChannelMessage(new SendChannelMessageCommand(
                command.accountId(),
                command.channelId(),
                new TextChannelMessageDraft(command.body())
        ));
    }

    /**
     * 转发频道消息。
     */
    public ChannelMessageResult forwardChannelMessage(ForwardChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.sourceMessageId(), "sourceMessageId");
        requirePositive(command.targetChannelId(), "targetChannelId");
        if (command.comment() != null && command.comment().trim().length() > 500) {
            throw ProblemException.validationFailed("comment length must be less than or equal to 500");
        }
        ChannelMessage sourceMessage = requireMessage(command.sourceMessageId());
        Channel targetChannel = requireChannel(command.targetChannelId());
        requireMembership(targetChannel.id(), command.accountId());
        StringBuilder forwardedText = new StringBuilder();
        if (command.comment() != null && !command.comment().isBlank()) {
            forwardedText.append(command.comment().trim()).append("\n\n");
        }
        forwardedText.append("[Forwarded] ").append(sourceMessage.previewText() == null ? "" : sourceMessage.previewText());
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProperties.id(),
                    targetChannel.conversationId(),
                    targetChannel.id(),
                    command.accountId(),
                    TEXT_MESSAGE_TYPE,
                    forwardedText.toString(),
                    forwardedText.toString(),
                    forwardedText.toString(),
                    null,
                    null,
                    null,
                    normalizeForwardedFrom(sourceMessage),
                    "sent",
                    now(),
                    null,
                    1L
            ));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(targetChannel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 编辑既有文本消息。
     * 输入：编辑人、目标消息、期望版本和新的文本 / mentions。
     * 输出：更新后的稳定消息结果。
     * 副作用：会递增编辑版本并广播 `message.updated` 事件。
     */
    public ChannelMessageResult editChannelMessage(EditChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.messageId(), "messageId");
        if (!CORE_TEXT_DOMAIN.equals(command.domain())) {
            throw ProblemException.validationFailed("schema_invalid", "domain is not supported");
        }
        if (!CORE_TEXT_DOMAIN_VERSION.equals(command.domainVersion())) {
            throw ProblemException.validationFailed("schema_invalid", "domain version is not supported");
        }
        if (command.text() == null || command.text().isBlank()) {
            throw ProblemException.validationFailed("validation_failed", "text must not be blank");
        }
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            Channel channel = requireChannel(existingMessage.channelId());
            requireMembership(channel.id(), command.accountId());
            requireEditable(existingMessage, command.accountId());
            if (command.expectedEditVersion() != null && command.expectedEditVersion() != existingMessage.editVersion()) {
                throw ProblemException.conflict("conflict", "message edit version conflict");
            }
            ChannelMessage updatedMessage = new ChannelMessage(
                    existingMessage.messageId(),
                    existingMessage.serverId(),
                    existingMessage.conversationId(),
                    existingMessage.channelId(),
                    existingMessage.senderId(),
                    existingMessage.messageType(),
                    command.text().trim(),
                    command.text().trim(),
                    command.text().trim(),
                    existingMessage.payload(),
                    existingMessage.metadata(),
                    normalizeMentions(command.mentions()),
                    existingMessage.forwardedFrom(),
                    existingMessage.status(),
                    existingMessage.createdAt(),
                    now(),
                    existingMessage.editVersion() + 1
            );
            ChannelMessage savedMessage = messageRepository.update(updatedMessage);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, existingMessage.mentions());
            PersistedMessage updatedResult = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageUpdatedAfterCommit(afterCommit, updatedResult);
            return updatedResult;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 通过 HTTP 发送频道消息。
     */
    public ChannelMessageResult sendChannelMessageHttp(SendChannelMessageHttpCommand command) {
        if (!CORE_TEXT_DOMAIN_VERSION.equals(command.domainVersion())) {
            throw ProblemException.validationFailed("schema_invalid", "domain version is not supported");
        }
        return switch (command.domain()) {
            case CORE_TEXT_DOMAIN -> sendHttpTextMessage(command);
            case "Core:File" -> sendHttpFileMessage(command);
            case "Core:Voice" -> sendHttpVoiceMessage(command);
            default -> throw ProblemException.validationFailed("schema_invalid", "domain is not supported");
        };
    }

    /**
     * 发送内部 system 频道消息。
     *
     * @param command system 消息发送命令
     * @return 已持久化并已用于实时分发的 system 消息结果
     */
    public ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command) {
        validateSystemSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            requireSystemChannel(channel);
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(SYSTEM_MESSAGE_TYPE);
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.operatorAccountId(),
                    now()
            ), new SystemChannelMessageDraft(command.body(), command.payload(), command.metadata()));
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 撤回频道消息。
     *
     * @param command 撤回命令
     * @return 撤回后的稳定消息结果
     */
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            ChannelMessage existingMessage = requireMessage(command.messageId());
            if (existingMessage.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            ChannelMember senderMember = channelMemberRepository.findByChannelIdAndAccountId(channel.id(), existingMessage.senderId())
                    .orElse(null);
            channelGovernancePolicy.requireCanRecallMessage(channel, operator, existingMessage, senderMember);
            if (isRecalled(existingMessage)) {
                PersistedMessage recalledResult = new PersistedMessage(
                        existingMessage,
                        snapshotSender(existingMessage.senderId()),
                        channelMemberRepository.findAccountIdsByChannelId(channel.id()),
                        List.of()
                );
                publishMessageUpdatedAfterCommit(afterCommit, recalledResult);
                return recalledResult;
            }
            ChannelMessage recalledMessage = messageRepository.update(toRecalledMessage(existingMessage));
            appendRecallAudit(channel.id(), operator.accountId(), recalledMessage.messageId(), recalledMessage.senderId());
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage recalledResult = new PersistedMessage(
                    recalledMessage,
                    snapshotSender(recalledMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageUpdatedAfterCommit(afterCommit, recalledResult);
            return recalledResult;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 硬删除频道消息。
     */
    public void deleteChannelMessage(DeleteChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            Channel channel = requireChannel(existingMessage.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            ChannelMember senderMember = channelMemberRepository.findByChannelIdAndAccountId(channel.id(), existingMessage.senderId()).orElse(null);
            channelGovernancePolicy.requireCanRecallMessage(channel, operator, existingMessage, senderMember);
            messageRepository.delete(existingMessage.messageId());
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage deletedMessage = new PersistedMessage(
                    toRecalledMessage(existingMessage),
                    snapshotSender(existingMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageUpdatedAfterCommit(afterCommit, deletedMessage);
        });
    }

    /**
     * 置顶频道消息。
     * 输入：操作人、频道、消息和可选 note。
     * 输出：新生成的置顶结果。
     * 约束：单频道置顶数量受上限控制。
     */
    public ChannelPinResult pinChannelMessage(PinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        if (command.note() != null && command.note().trim().length() > 200) {
            throw ProblemException.validationFailed("note length must be less than or equal to 200");
        }
        PinnedChannelMessage pinnedChannelMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanModeratePin(channel, operator);
            ChannelMessage message = requireMessage(command.messageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            if (channelPinRepository.findByChannelIdAndMessageId(channel.id(), message.messageId()).isEmpty()
                    && channelPinRepository.countByChannelId(channel.id()) >= MAX_PINS_PER_CHANNEL) {
                throw ProblemException.validationFailed("pin_limit_reached", "channel pin limit is reached");
            }
            ChannelPin pin = new ChannelPin(idGenerator.nextLongId(), channel.id(), message.messageId(), operator.accountId(), command.note() == null ? "" : command.note().trim(), now());
            channelPinRepository.save(pin);
            PinnedChannelMessage pinnedResult = new PinnedChannelMessage(pin, channelMemberRepository.findAccountIdsByChannelId(channel.id()));
            publishMessagePinnedAfterCommit(afterCommit, pinnedResult);
            return pinnedResult;
        });
        return toPinResult(pinnedChannelMessage.pin());
    }

    /**
     * 取消频道消息置顶。
     * 副作用：会删除置顶记录并广播 `message.unpinned` 事件。
     */
    public void unpinChannelMessage(UnpinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanModeratePin(channel, operator);
            ChannelPin pin = channelPinRepository.findByChannelIdAndMessageId(channel.id(), command.messageId())
                    .orElseThrow(() -> ProblemException.notFound("channel pin does not exist"));
            channelPinRepository.delete(channel.id(), command.messageId());
            UnpinnedChannelMessage unpinnedChannelMessage = new UnpinnedChannelMessage(
                    pin,
                    operator.accountId(),
                    now().toEpochMilli(),
                    channelMemberRepository.findAccountIdsByChannelId(channel.id())
            );
            publishMessageUnpinnedAfterCommit(afterCommit, unpinnedChannelMessage);
        });
    }

    /**
     * 查询频道置顶列表。
     * 输入：频道、分页游标与分页大小。
     * 输出：按倒序返回的置顶结果集合。
     */
    public List<ChannelPinResult> listChannelPins(ListChannelPinsQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        return channelPinRepository.findByChannelIdBefore(channel.id(), query.cursorMessageId(), query.limit() + 1).stream()
                .map(this::toPinResult)
                .toList();
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
        if (query.aroundMessageId() != null) {
            ChannelMessage targetMessage = requireMessage(query.aroundMessageId());
            if (targetMessage.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            int beforeCount = query.before() == null ? 25 : query.before();
            int afterCount = query.after() == null ? 25 : query.after();
            List<ChannelMessageResult> messages = java.util.stream.Stream.concat(
                            java.util.stream.Stream.concat(
                                    messageRepository.findByChannelIdBefore(channel.id(), query.aroundMessageId(), beforeCount).stream(),
                                    java.util.stream.Stream.of(targetMessage)
                            ),
                            messageRepository.findByChannelIdAfter(channel.id(), query.aroundMessageId(), afterCount).stream()
                    )
                    .sorted(java.util.Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .map(this::toResult)
                    .toList();
            return new ChannelMessageHistoryResult(messages, null);
        }
        List<ChannelMessageResult> messages = messageRepository.findByChannelIdBefore(
                        channel.id(),
                        query.cursorMessageId(),
                        query.limit() + 1
                ).stream()
                .map(this::toResult)
                .toList();
        boolean hasMore = messages.size() > query.limit();
        List<ChannelMessageResult> pageItems = hasMore ? messages.subList(0, query.limit()) : messages;
        Long nextCursor = hasMore ? pageItems.get(pageItems.size() - 1).messageId() : null;
        return new ChannelMessageHistoryResult(pageItems, nextCursor);
    }

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param query 搜索参数
     * @return 搜索结果
     */
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        validateSearchQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.searchByChannelId(
                        channel.id(),
                        query.keyword().trim(),
                        query.cursorMessageId(),
                        query.senderAccountId(),
                        normalizeDomain(query.domain()),
                        query.beforeMessageId(),
                        query.afterMessageId(),
                        query.limit() + 1
                ).stream()
                .map(this::toResult)
                .toList();
        return new ChannelMessageSearchResult(messages);
    }

    private void validateSendCommand(SendChannelTextMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.body() == null || command.body().isBlank()) {
            throw ProblemException.validationFailed("body must not be blank");
        }
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }

    private void validateSystemSendCommand(SendSystemChannelMessageCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    private void validateSendCommand(SendChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.draft() == null) {
            throw ProblemException.validationFailed("draft must not be null");
        }
        if (command.draft().type() == null || command.draft().type().isBlank()) {
            throw ProblemException.validationFailed("message type must not be blank");
        }
    }

    private void validateHistoryQuery(GetChannelMessageHistoryQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.aroundMessageId() != null && query.aroundMessageId() <= 0) {
            throw ProblemException.validationFailed("around_mid must be greater than 0");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursorMessageId must be greater than 0");
        }
        if (query.aroundMessageId() != null && query.cursorMessageId() != null) {
            throw ProblemException.validationFailed("cursor and around_mid cannot be used together");
        }
        if (query.before() != null && (query.before() < 0 || query.before() > 50)) {
            throw ProblemException.validationFailed("before must be between 0 and 50");
        }
        if (query.after() != null && (query.after() < 0 || query.after() > 50)) {
            throw ProblemException.validationFailed("after must be between 0 and 50");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
    }

    private void validateSearchQuery(SearchChannelMessagesQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.keyword() == null || query.keyword().isBlank()) {
            throw ProblemException.validationFailed("keyword must not be blank");
        }
        if (query.keyword().trim().length() > 100) {
            throw ProblemException.validationFailed("keyword length must be between 1 and 100");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.senderAccountId() != null && query.senderAccountId() <= 0) {
            throw ProblemException.validationFailed("sender_uid must be greater than 0");
        }
        if (query.beforeMessageId() != null && query.beforeMessageId() <= 0) {
            throw ProblemException.validationFailed("before_mid must be greater than 0");
        }
        if (query.afterMessageId() != null && query.afterMessageId() <= 0) {
            throw ProblemException.validationFailed("after_mid must be greater than 0");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return switch (domain.trim()) {
            case "Core:Text" -> TEXT_MESSAGE_TYPE;
            case "Core:File" -> FILE_MESSAGE_TYPE;
            case "Core:Voice" -> VOICE_MESSAGE_TYPE;
            default -> domain.trim();
        };
    }

    private ChannelMessageResult sendHttpTextMessage(SendChannelMessageHttpCommand command) {
        String normalizedText = requiredString(command.data(), "text", "text must not be blank");
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    TEXT_MESSAGE_TYPE,
                    normalizedText,
                    normalizedText,
                    normalizedText,
                    null,
                    null,
                    normalizeMentions(command.mentions()),
                    null,
                    "sent",
                    now(),
                    null,
                    1L
            ));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    private ChannelMessageResult sendHttpFileMessage(SendChannelMessageHttpCommand command) {
        String shareKey = requiredString(command.data(), "share_key", "share_key must not be blank");
        String filename = requiredString(command.data(), "filename", "filename must not be blank");
        String body = optionalString(command.data(), "text");
        Long size = optionalLong(command.data(), "size");
        String mimeType = optionalString(command.data(), "mime_type");
        return sendAttachmentHttpMessage(command, new FileChannelMessageDraft(
                body,
                resolveAttachmentObjectKey(shareKey),
                filename,
                mimeType,
                size,
                null
        ));
    }

    private ChannelMessageResult sendHttpVoiceMessage(SendChannelMessageHttpCommand command) {
        String shareKey = requiredString(command.data(), "share_key", "share_key must not be blank");
        String filename = requiredString(command.data(), "filename", "filename must not be blank");
        Long durationMillis = requiredLong(command.data(), "duration_millis", "duration_millis must be greater than 0");
        String body = optionalString(command.data(), "text");
        Long size = optionalLong(command.data(), "size");
        String mimeType = optionalString(command.data(), "mime_type");
        String transcript = optionalString(command.data(), "transcript");
        return sendAttachmentHttpMessage(command, new VoiceChannelMessageDraft(
                body,
                resolveAttachmentObjectKey(shareKey),
                filename,
                mimeType,
                size,
                durationMillis,
                transcript,
                null
        ));
    }

    private ChannelMessageResult sendAttachmentHttpMessage(
            SendChannelMessageHttpCommand command,
            team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft draft
    ) {
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(draft.type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), draft);
            ChannelMessage savedMessage = messageRepository.save(withMentions(message, command.mentions()));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    private void publishMessageCreatedAfterCommit(AfterCommitExecutor afterCommit, PersistedMessage persistedMessage) {
            afterCommit.execute(() -> {
            messageRealtimePublisher.publish(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    private void publishMessageUpdatedAfterCommit(AfterCommitExecutor afterCommit, PersistedMessage persistedMessage) {
        afterCommit.execute(() -> {
            messageRealtimePublisher.publishUpdate(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    private void publishMessagePinnedAfterCommit(AfterCommitExecutor afterCommit, PinnedChannelMessage pinnedChannelMessage) {
        afterCommit.execute(() -> messageRealtimePublisher.publishPin(
                pinnedChannelMessage.pin(),
                pinnedChannelMessage.recipientAccountIds()
        ));
    }

    private void publishMessageUnpinnedAfterCommit(AfterCommitExecutor afterCommit, UnpinnedChannelMessage unpinnedChannelMessage) {
        afterCommit.execute(() -> messageRealtimePublisher.publishUnpin(
                unpinnedChannelMessage.pin(),
                unpinnedChannelMessage.unpinnedByAccountId(),
                unpinnedChannelMessage.unpinnedAt(),
                unpinnedChannelMessage.recipientAccountIds()
        ));
    }

    private ChannelMessage withMentions(ChannelMessage message, List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
        return new ChannelMessage(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                message.searchableText(),
                message.payload(),
                message.metadata(),
                normalizeMentions(mentions),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    private String resolveAttachmentObjectKey(String shareKey) {
        return FileShareKeyCodec.resolveObjectKey(shareKey);
    }

    private String requiredString(java.util.Map<String, Object> data, String fieldName, String message) {
        String value = optionalString(data, fieldName);
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    private String optionalString(java.util.Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long requiredLong(java.util.Map<String, Object> data, String fieldName, String message) {
        Long value = optionalLong(data, fieldName);
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    private Long optionalLong(java.util.Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("validation_failed", fieldName + " must be a number");
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private void requireSystemChannel(Channel channel) {
        if (!SYSTEM_MESSAGE_TYPE.equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_required", "system message requires system channel");
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
                message.body(),
                message.previewText(),
                messageAttachmentPayloadResolver.resolve(message.messageType(), message.payload()),
                message.metadata(),
                message.mentions(),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private ChannelPinResult toPinResult(ChannelPin pin) {
        return new ChannelPinResult(pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.pinnedAt(), pin.note());
    }

    private MessageSenderSnapshot snapshotSender(long accountId) {
        return userProfileRepository.findByAccountId(accountId)
                .map(profile -> new MessageSenderSnapshot(profile.accountId(), profile.nickname(), profile.avatarUrl()))
                .orElseGet(() -> new MessageSenderSnapshot(accountId, "", ""));
    }

    private ChannelMessage toRecalledMessage(ChannelMessage message) {
        return new ChannelMessage(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                RECALLED_MESSAGE_PLACEHOLDER,
                RECALLED_MESSAGE_PLACEHOLDER,
                "",
                null,
                null,
                null,
                message.forwardedFrom(),
                RECALLED_STATUS,
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    private void appendRecallAudit(long channelId, long actorAccountId, long messageId, long senderId) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                nextMessageId(),
                channelId,
                actorAccountId,
                MESSAGE_RECALLED_AUDIT_ACTION,
                senderId,
                "{\"messageId\":" + messageId + ",\"senderAccountId\":" + senderId + "}",
                now()
        ));
    }

    private boolean isRecalled(ChannelMessage message) {
        return RECALLED_STATUS.equals(message.status());
    }

    private void requireEditable(ChannelMessage message, long accountId) {
        if (message.senderId() != accountId || isRecalled(message)) {
            throw ProblemException.forbidden("message_not_editable", "message is not editable");
        }
        if (message.createdAt().plusSeconds(MESSAGE_EDIT_WINDOW_SECONDS).isBefore(now())) {
            throw ProblemException.forbidden("message_edit_window_expired", "message edit window expired");
        }
    }

    private String normalizeMentions(List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            return null;
        }
        List<java.util.Map<String, Object>> normalizedMentions = mentions.stream()
                .map(mention -> java.util.Map.<String, Object>of(
                        "type", mention.type(),
                        "uid", Long.toString(mention.uid())
                ))
                .distinct()
                .toList();
        return jsonProvider.toJson(normalizedMentions);
    }

    private List<Mention> persistMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        List<Mention> mentions = buildMentions(message, recipientAccountIds, previousMentionsJson);
        for (Mention mention : mentions) {
            mentionRepository.save(mention);
        }
        return mentions;
    }

    private void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            messageRealtimePublisher.publishMentionCreated(mention, List.of(mention.targetAccountId()));
        }
    }

    private List<Mention> buildMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        if (message.mentions() == null || message.mentions().isBlank()) {
            return List.of();
        }
        java.util.Set<String> existingMentionKeys = mentionKeys(previousMentionsJson);
        java.util.Set<Long> validRecipients = new java.util.HashSet<>(recipientAccountIds);
        List<Mention> mentions = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> item : jsonProvider.fromJson(message.mentions(), new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
        })) {
            String type = item.get("type") == null ? null : String.valueOf(item.get("type")).trim();
            String uid = item.get("uid") == null ? null : String.valueOf(item.get("uid")).trim();
            if (!"user".equals(type) || uid == null || uid.isBlank()) {
                continue;
            }
            long targetAccountId;
            try {
                targetAccountId = Long.parseLong(uid);
            } catch (NumberFormatException exception) {
                continue;
            }
            if (targetAccountId == message.senderId() || !validRecipients.contains(targetAccountId)) {
                continue;
            }
            String mentionKey = type + ":" + targetAccountId;
            if (existingMentionKeys.contains(mentionKey)) {
                continue;
            }
            existingMentionKeys.add(mentionKey);
            mentions.add(new Mention(
                    idGenerator.nextLongId(),
                    message.channelId(),
                    message.messageId(),
                    message.senderId(),
                    type,
                    targetAccountId,
                    now(),
                    false
            ));
        }
        return mentions;
    }

    private java.util.Set<String> mentionKeys(String mentionsJson) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        if (mentionsJson == null || mentionsJson.isBlank()) {
            return keys;
        }
        for (java.util.Map<String, Object> item : jsonProvider.fromJson(mentionsJson, new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
        })) {
            Object type = item.get("type");
            Object uid = item.get("uid");
            if (type != null && uid != null) {
                keys.add(String.valueOf(type).trim() + ":" + String.valueOf(uid).trim());
            }
        }
        return keys;
    }

    private String normalizeForwardedFrom(ChannelMessage sourceMessage) {
        return jsonProvider.toJson(java.util.Map.of(
                "mid", Long.toString(sourceMessage.messageId()),
                "cid", Long.toString(sourceMessage.channelId()),
                "uid", Long.toString(sourceMessage.senderId()),
                "preview", sourceMessage.previewText() == null ? "" : sourceMessage.previewText(),
                "send_time", sourceMessage.createdAt().toEpochMilli()
        ));
    }

    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    private String resolveContentType(String messageType, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        return VOICE_MESSAGE_TYPE.equals(messageType) ? "audio/mpeg" : "application/octet-stream";
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

    private record PersistedMessage(
            ChannelMessage message,
            MessageSenderSnapshot senderSnapshot,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
    }

    private record PinnedChannelMessage(ChannelPin pin, List<Long> recipientAccountIds) {
    }

    private record UnpinnedChannelMessage(ChannelPin pin, long unpinnedByAccountId, long unpinnedAt, List<Long> recipientAccountIds) {
    }
}
