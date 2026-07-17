package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageHttpCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：直接承载频道消息创建、HTTP 投递、系统消息和消息转发用例。
 * 边界：不承载消息编辑、撤回、删除、历史查询、搜索、附件上传和置顶能力。
 */
@Service
public class ChannelMessagePublishingDomainApi extends AbstractMessageDomainSupport implements ChannelMessagePublishingApi {

    private final MessageHttpPayloadParser messageHttpPayloadParser;
    private final MessageDeliveryCommandValidator messageDeliveryCommandValidator;

    public ChannelMessagePublishingDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        super(
                messageChannelBoundary,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
        this.messageHttpPayloadParser = new MessageHttpPayloadParser();
        this.messageDeliveryCommandValidator = new MessageDeliveryCommandValidator();
    }

    @Override
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        messageDeliveryCommandValidator.validateSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            MessageChannelBoundary.MessageChannel channel = requireSendableChannel(command.channelId(), command.accountId());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(command.draft().type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), command.draft());
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            List<Mention> mentions = messageMentionManager.persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    @Override
    public ChannelMessageResult sendChannelTextMessage(SendChannelTextMessageCommand command) {
        messageDeliveryCommandValidator.validateSendCommand(command);
        return sendChannelMessage(new SendChannelMessageCommand(
                command.accountId(),
                command.channelId(),
                new TextChannelMessageDraft(command.body())
        ));
    }

    @Override
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

    @Override
    public ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command) {
        messageDeliveryCommandValidator.validateSystemSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            MessageChannelBoundary.MessageChannel channel = requireChannel(command.channelId());
            requireSystemChannel(channel);
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(SYSTEM_MESSAGE_TYPE);
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.operatorAccountId(),
                    now()
            ), new SystemChannelMessageDraft(command.body(), command.payload(), command.metadata()));
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    @Override
    public ChannelMessageResult forwardChannelMessage(ForwardChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.sourceMessageId(), "sourceMessageId");
        requirePositive(command.targetChannelId(), "targetChannelId");
        if (command.comment() != null && command.comment().trim().length() > 500) {
            throw ProblemException.validationFailed("comment length must be less than or equal to 500");
        }
        ChannelMessage sourceMessage = requireMessage(command.sourceMessageId());
        requireMemberChannel(sourceMessage.channelId(), command.accountId());
        MessageChannelBoundary.MessageChannel targetChannel = requireSendableChannel(command.targetChannelId(), command.accountId());
        StringBuilder forwardedText = new StringBuilder();
        if (command.comment() != null && !command.comment().isBlank()) {
            forwardedText.append(command.comment().trim()).append("\n\n");
        }
        forwardedText.append("[Forwarded] ").append(sourceMessage.previewText() == null ? "" : sourceMessage.previewText());
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProvider.id(),
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
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(targetChannel.id());
            List<Mention> mentions = messageMentionManager.persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 处理 HTTP 文本消息发送分支。
     * 副作用：在事务内保存消息、持久化 mention，并登记事务提交后的实时发布动作。
     *
     * @param command HTTP 消息发送命令
     * @return 创建后的消息投影
     */
    private ChannelMessageResult sendHttpTextMessage(SendChannelMessageHttpCommand command) {
        String normalizedText = messageHttpPayloadParser.requiredString(command.data(), "text", "text must not be blank");
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            MessageChannelBoundary.MessageChannel channel = requireSendableChannel(command.channelId(), command.accountId());
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    TEXT_MESSAGE_TYPE,
                    normalizedText,
                    normalizedText,
                    normalizedText,
                    null,
                    null,
                    messageMentionManager.normalizeMentions(command.mentions()),
                    null,
                    "sent",
                    now(),
                    null,
                    1L
            ));
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            List<Mention> mentions = messageMentionManager.persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    /**
     * 处理 HTTP 文件消息发送分支。
     * 约束：协议 data 必须提供文件名，并通过 share_key 或 object_key 解析附件对象。
     *
     * @param command HTTP 消息发送命令
     * @return 创建后的消息投影
     */
    private ChannelMessageResult sendHttpFileMessage(SendChannelMessageHttpCommand command) {
        String filename = messageHttpPayloadParser.requiredString(command.data(), "filename", "filename must not be blank");
        String body = messageHttpPayloadParser.optionalString(command.data(), "text");
        Long size = messageHttpPayloadParser.optionalLong(command.data(), "size");
        String mimeType = messageHttpPayloadParser.optionalString(command.data(), "mime_type");
        return sendAttachmentHttpMessage(command, new FileChannelMessageDraft(
                body,
                messageHttpPayloadParser.resolveAttachmentObjectKey(command.data()),
                filename,
                mimeType,
                size,
                null
        ));
    }

    /**
     * 处理 HTTP 语音消息发送分支。
     * 约束：协议 data 必须提供文件名和正数语音时长，并解析为 voice 草稿。
     *
     * @param command HTTP 消息发送命令
     * @return 创建后的消息投影
     */
    private ChannelMessageResult sendHttpVoiceMessage(SendChannelMessageHttpCommand command) {
        String filename = messageHttpPayloadParser.requiredString(command.data(), "filename", "filename must not be blank");
        Long durationMillis = messageHttpPayloadParser.requiredLong(command.data(), "duration_millis", "duration_millis must be greater than 0");
        String body = messageHttpPayloadParser.optionalString(command.data(), "text");
        Long size = messageHttpPayloadParser.optionalLong(command.data(), "size");
        String mimeType = messageHttpPayloadParser.optionalString(command.data(), "mime_type");
        String transcript = messageHttpPayloadParser.optionalString(command.data(), "transcript");
        return sendAttachmentHttpMessage(command, new VoiceChannelMessageDraft(
                body,
                messageHttpPayloadParser.resolveAttachmentObjectKey(command.data()),
                filename,
                mimeType,
                size,
                durationMillis,
                transcript,
                null
        ));
    }

    /**
     * 发送由 HTTP data 转换出的附件消息草稿。
     * 副作用：在事务内校验频道发送权限、通过消息插件构建领域消息、保存消息并登记实时发布。
     *
     * @param command HTTP 消息发送命令
     * @param draft 已按消息类型解析出的附件消息草稿
     * @return 创建后的消息投影
     */
    private ChannelMessageResult sendAttachmentHttpMessage(
            SendChannelMessageHttpCommand command,
            ChannelMessageDraft draft
    ) {
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            MessageChannelBoundary.MessageChannel channel = requireSendableChannel(command.channelId(), command.accountId());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(draft.type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), draft);
            ChannelMessage savedMessage = messageRepository.save(withMentions(message, command.mentions()));
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            List<Mention> mentions = messageMentionManager.persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            messageAfterCommitPublisher.publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }
}
