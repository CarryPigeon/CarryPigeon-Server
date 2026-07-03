package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * message 命令侧领域 API 共享支撑。
 * 职责：收敛发送与治理 API 共用的仓储访问、结果投影、事件发布和基础校验逻辑。
 * 边界：这里只提供内部复用能力，不作为 feature 对外暴露的稳定应用入口。
 */
abstract class AbstractMessageDomainSupport {

    protected static final String SYSTEM_MESSAGE_TYPE = "system";
    protected static final String FILE_MESSAGE_TYPE = "file";
    protected static final String VOICE_MESSAGE_TYPE = "voice";
    protected static final String TEXT_MESSAGE_TYPE = "text";
    protected static final String CORE_TEXT_DOMAIN = "Core:Text";
    protected static final String CORE_TEXT_DOMAIN_VERSION = "1.0.0";
    protected static final String RECALLED_STATUS = "recalled";
    protected static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    protected static final long MAX_PINS_PER_CHANNEL = 50L;
    protected static final long MESSAGE_EDIT_WINDOW_SECONDS = 300L;
    protected static final String RECALLED_MESSAGE_PLACEHOLDER = "[消息已撤回]";

    protected final MessageChannelBoundary messageChannelBoundary;
    protected final MessageRepository messageRepository;
    protected final UserProfileRepository userProfileRepository;
    protected final MessageMentionManager messageMentionManager;
    protected final MessageAfterCommitPublisher messageAfterCommitPublisher;
    protected final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    protected final MessagePayloadResolver messageAttachmentPayloadResolver;
    protected final ServerIdentityProvider serverIdentityProvider;
    protected final IdGenerator idGenerator;
    protected final JsonProvider jsonProvider;
    protected final TimeProvider timeProvider;
    protected final TransactionRunner transactionRunner;

    protected AbstractMessageDomainSupport(
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
        this.messageChannelBoundary = messageChannelBoundary;
        this.messageRepository = messageRepository;
        this.userProfileRepository = userProfileRepository;
        this.messageMentionManager = new MessageMentionManager(mentionRepository, idGenerator, jsonProvider, timeProvider);
        this.messageAfterCommitPublisher = new MessageAfterCommitPublisher(messageRealtimePublisher);
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.serverIdentityProvider = serverIdentityProvider;
        this.idGenerator = idGenerator;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 基于已有消息重建 mention 信息。
     * 语义：编辑消息时只替换标准化后的 mention 列表，不改变消息主体、状态和版本字段。
     *
     * @param message 原频道消息
     * @param mentions 编辑命令携带的 mention 目标
     * @return 更新 mention 后的频道消息
     */
    protected ChannelMessage withMentions(ChannelMessage message, List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
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
                messageMentionManager.normalizeMentions(mentions),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    /**
     * 读取必须存在的频道快照。
     * 边界：通过 channel feature 边界端口访问频道，不直接依赖频道仓储。
     *
     * @param channelId 频道 ID
     * @return 消息侧频道快照
     */
    protected MessageChannelBoundary.MessageChannel requireChannel(long channelId) {
        return messageChannelBoundary.requireChannel(channelId);
    }

    /**
     * 读取当前账号必须加入的频道快照。
     * 失败语义：账号不是频道成员时由 channel 边界端口表达权限问题。
     *
     * @param channelId 频道 ID
     * @param accountId 当前账号 ID
     * @return 消息侧频道快照
     */
    protected MessageChannelBoundary.MessageChannel requireMemberChannel(long channelId, long accountId) {
        return messageChannelBoundary.requireMemberChannel(channelId, accountId);
    }

    /**
     * 读取当前账号可发送消息的频道快照。
     * 约束：会同时校验成员关系、禁言状态和频道发送边界。
     *
     * @param channelId 频道 ID
     * @param accountId 当前账号 ID
     * @return 可发送消息的频道快照
     */
    protected MessageChannelBoundary.MessageChannel requireSendableChannel(long channelId, long accountId) {
        return messageChannelBoundary.requireSendableChannel(channelId, accountId, now());
    }

    /**
     * 校验频道必须为 system 频道。
     *
     * @param channel 待校验频道快照
     */
    protected void requireSystemChannel(MessageChannelBoundary.MessageChannel channel) {
        messageChannelBoundary.requireSystemChannel(channel);
    }

    /**
     * 将频道消息聚合转换为消息结果投影。
     * 边界：附件类 payload 在这里通过解析端口统一转换为稳定输出结构。
     *
     * @param message 频道消息聚合
     * @return 频道消息结果投影
     */
    protected ChannelMessageResult toResult(ChannelMessage message) {
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

    /**
     * 读取必须存在的频道消息。
     * 失败语义：消息不存在时抛出 not found 问题。
     *
     * @param messageId 消息 ID
     * @return 频道消息聚合
     */
    protected ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    /**
     * 将频道置顶记录转换为消息侧置顶投影。
     *
     * @param pin 频道置顶记录
     * @return 频道置顶结果投影
     */
    protected ChannelPinResult toPinResult(MessageChannelBoundary.MessageChannelPin pin) {
        return new ChannelPinResult(pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.pinnedAt(), pin.note());
    }

    /**
     * 获取消息发送者展示快照。
     * 语义：发送者资料缺失时保留账号 ID，并使用空展示字段，避免消息发布被资料缺口阻断。
     *
     * @param accountId 发送者账号 ID
     * @return 发送者快照
     */
    protected MessageSenderSnapshot snapshotSender(long accountId) {
        return userProfileRepository.findByAccountId(accountId)
                .map(profile -> new MessageSenderSnapshot(profile.accountId(), profile.nickname(), profile.avatarUrl()))
                .orElseGet(() -> new MessageSenderSnapshot(accountId, "", ""));
    }

    /**
     * 构造撤回后的消息状态。
     * 语义：保留消息身份和时间信息，清空可检索内容与 payload，并用固定占位文本替代正文。
     *
     * @param message 原频道消息
     * @return 撤回状态消息
     */
    protected ChannelMessage toRecalledMessage(ChannelMessage message) {
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

    /**
     * 追加消息撤回审计记录。
     * 边界：审计写入通过 channel 边界端口完成，message feature 不直接访问频道审计仓储。
     *
     * @param channelId 频道 ID
     * @param actorAccountId 执行撤回的账号 ID
     * @param messageId 被撤回消息 ID
     * @param senderId 被撤回消息发送者账号 ID
     */
    protected void appendRecallAudit(long channelId, long actorAccountId, long messageId, long senderId) {
        messageChannelBoundary.appendMessageRecalledAudit(
                nextMessageId(),
                channelId,
                actorAccountId,
                messageId,
                senderId,
                now()
        );
    }

    /**
     * 判断消息是否处于撤回状态。
     *
     * @param message 频道消息
     * @return true 表示消息已撤回
     */
    protected boolean isRecalled(ChannelMessage message) {
        return RECALLED_STATUS.equals(message.status());
    }

    /**
     * 校验消息是否允许被当前账号编辑。
     * 约束：仅发送者可在编辑窗口内编辑，已撤回消息不可编辑。
     *
     * @param message 待编辑消息
     * @param accountId 当前账号 ID
     */
    protected void requireEditable(ChannelMessage message, long accountId) {
        if (message.senderId() != accountId || isRecalled(message)) {
            throw ProblemException.forbidden("message_not_editable", "message is not editable");
        }
        if (message.createdAt().plusSeconds(MESSAGE_EDIT_WINDOW_SECONDS).isBefore(now())) {
            throw ProblemException.forbidden("message_edit_window_expired", "message edit window expired");
        }
    }

    /**
     * 生成转发来源元数据。
     * 语义：保留源消息、源频道、源发送者、预览文本和发送时间，供投影层展示转发来源。
     *
     * @param sourceMessage 被转发的源消息
     * @return 转发来源 JSON 文本
     */
    protected String normalizeForwardedFrom(ChannelMessage sourceMessage) {
        return jsonProvider.toJson(Map.of(
                "mid", Long.toString(sourceMessage.messageId()),
                "cid", Long.toString(sourceMessage.channelId()),
                "uid", Long.toString(sourceMessage.senderId()),
                "preview", sourceMessage.previewText() == null ? "" : sourceMessage.previewText(),
                "send_time", sourceMessage.createdAt().toEpochMilli()
        ));
    }

    /**
     * 获取新的消息领域 ID。
     *
     * @return 全局唯一长整型 ID
     */
    protected long nextMessageId() {
        return idGenerator.nextLongId();
    }

    /**
     * 获取消息领域服务统一时间。
     *
     * @return 当前时间
     */
    protected Instant now() {
        return timeProvider.nowInstant();
    }

    /**
     * 校验 ID 或数值入参必须为正数。
     *
     * @param value 待校验数值
     * @param fieldName 字段名，用于生成校验失败消息
     */
    protected void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    /**
     * 已持久化消息的发布上下文。
     * 职责：把消息、发送者快照、接收人快照和 mention 列表作为事务后事件输入一起返回。
     *
     * @param message 已保存的频道消息
     * @param senderSnapshot 发送者展示快照
     * @param recipientAccountIds 接收事件的账号 ID 列表
     * @param mentions 本次消息产生的 mention 列表
     */
    protected record PersistedMessage(
            ChannelMessage message,
            MessageSenderSnapshot senderSnapshot,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
    }

    /**
     * 已置顶消息的发布上下文。
     *
     * @param pin 频道置顶记录
     * @param recipientAccountIds 接收置顶事件的账号 ID 列表
     */
    protected record PinnedChannelMessage(MessageChannelBoundary.MessageChannelPin pin, List<Long> recipientAccountIds) {
    }

    /**
     * 已取消置顶消息的发布上下文。
     *
     * @param pin 被取消的频道置顶记录
     * @param unpinnedByAccountId 执行取消置顶的账号 ID
     * @param unpinnedAt 取消置顶时间戳
     * @param recipientAccountIds 接收取消置顶事件的账号 ID 列表
     */
    protected record UnpinnedChannelMessage(
            MessageChannelBoundary.MessageChannelPin pin,
            long unpinnedByAccountId,
            long unpinnedAt,
            List<Long> recipientAccountIds
    ) {
    }
}
