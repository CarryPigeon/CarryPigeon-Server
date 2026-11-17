package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageSendRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageDispatcher;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPMessageStatus;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：编排消息发送、撤回、持久化与提交后通知。
 * 边界：不承载请求字段校验；只处理频道权限、禁言、domain 支持、消息状态和事务。
 */
@Service
public class ChannelMessageService implements ChannelMessageApi {

    private static final String MENTION_TARGET_TYPE_USER = "user";

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final MessageEventNotifier messageEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final CPChannelMessageDispatcher channelMessageDispatcher;
    private final ObjectWriter requestWriter;

    public ChannelMessageService(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageEventNotifier messageEventNotifier,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            CPChannelMessageDispatcher channelMessageDispatcher,
            ObjectMapper objectMapper
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.messageEventNotifier = messageEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.channelMessageDispatcher = channelMessageDispatcher;
        this.requestWriter = objectMapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    @Override
    public ChannelMessageResult sendChannelMessage(long accountId, long channelId, UUID requestId, String domain, Map<String, Object> data, List<Long> mentions) {
        if (requestId == null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "request_id is required");
        }
        // 提及在业务上是集合；缺省与空列表、元素顺序和重复项不形成不同的发送请求。
        List<Long> requestedMentions = mentions == null ? List.of()
                : mentions.stream().filter(Objects::nonNull).distinct().sorted().toList();
        String payloadHash = payloadHash(domain, data, requestedMentions);
        CPChannelMessage message = transactionRunner.runInTransaction(afterCommit -> {
            // 拉取上下文
            ChannelContext channelContext = channelContextApi.requireMemberContextForUpdate(channelId, accountId);
            long proposedMessageId = idGenerator.nextLongId();
            MessageSendRequest reserved = messageRepository.reserveSend(accountId, channelId, requestId, payloadHash, proposedMessageId);
            if (!reserved.payloadHash().equals(payloadHash)) {
                throw ProblemException.of(MessageProblem.SEND_REQUEST_CONFLICT);
            }
            if (reserved.messageId() != proposedMessageId) {
                // 返回当前消息状态，已撤回的内容不能通过重试恢复；不再产生提及、序号和创建通知。
                return messageRepository.findByIdForUpdate(reserved.messageId())
                        .orElseThrow(() -> ProblemException.of(MessageProblem.SEND_RESULT_GONE));
            }
            // 检查权限
            if (channelContext.isMuted()) {
                throw ProblemException.of(MessageProblem.USER_MUTED);
            }
            // 存在性校验
            if (!channelMessageDispatcher.supports(domain)) {
                throw ProblemException.of(MessageProblem.UNSUPPORTED_DOMAIN);
            }
            // 只校验请求中的有限提及集合；广播由在线索引路由，不再加载全频道成员。
            List<Long> normalizedMentions = channelContextApi.filterMemberAccountIds(channelId, requestedMentions);
            // 分配序号后持锁到消息、提及一并提交，后续序号不能先提交可见。
            // 组装消息
            CPChannelMessage originalMessage = new CPChannelMessage(
                    proposedMessageId,
                    accountId,
                    channelId,
                    messageRepository.nextSequence(channelId),
                    domain,
                    data,
                    timeProvider.nowInstant(),
                    normalizedMentions,
                    "",
                    "",
                    CPMessageStatus.SENT
            );
            // 处理消息
            CPChannelMessageHandler handler = channelMessageDispatcher.dispatch(domain);
            // 持久化消息
            CPChannelMessage finalMessage = handler.handle(originalMessage);
            // 空返回是已注册处理器违反契约，不能落入仓储并产生无关空指针；整个发送事务必须回滚。
            if (finalMessage == null) {
                throw new IllegalStateException("Message handler returned null: domain=" + domain
                        + ", handler=" + handler.getClass().getName());
            }
            messageRepository.save(finalMessage);
            // 生成提及记录；只保留当前频道成员，避免产生无法访问的提及
            List<Mention> savedMentions = finalMessage.mentions().stream()
                    .map(targetAccountId -> new Mention(
                            idGenerator.nextLongId(),
                            channelId,
                            finalMessage.messageId(),
                            accountId,
                            MENTION_TARGET_TYPE_USER,
                            targetAccountId,
                            finalMessage.sendTime(),
                            false
                    ))
                    .toList();
            if (!savedMentions.isEmpty()) {
                mentionRepository.saveAll(savedMentions);
            }
            // 创建事件与消息在同一事务登记，后台按状态派发，断线由持久化事件流恢复。
            messageEventNotifier.messageCreated(finalMessage);
            // 提及事件只广播给被提及账号
            savedMentions.forEach(mention -> messageEventNotifier.mentionCreated(afterCommit, mention));
            return finalMessage;
        });
        AuditLogs.success("message.send", accountId,
                "channel_id", channelId,
                "message_id", message.messageId(),
                "domain", domain);
        return ChannelMessageResult.of(message);
    }

    /** 按键排序覆盖嵌套 JSON 对象，摘要只包含原请求，不包含会随成员资料变化的派生快照。 */
    private String payloadHash(String domain, Map<String, Object> data, List<Long> mentions) {
        try {
            byte[] payload = requestWriter.writeValueAsBytes(Map.of("domain", domain, "data", data, "mentions", mentions));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException exception) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK SHA-256 is unavailable", exception);
        }
    }

    @Override
    public void recallChannelMessage(long accountId, long channelId, long messageId) {
        transactionRunner.runInTransaction(() -> {
            // 与发送、频道删除保持相同锁序；频道撤下后不能再更新消息或创建事件。
            ChannelContext operatorContext = channelContextApi.requireMemberContextForUpdate(channelId, accountId);
            // 查询已有的消息
            CPChannelMessage existingMessage = messageRepository.findById(messageId)
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            // 验证消息属于该频道
            if (existingMessage.channelId() != channelId) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            // 验证操作权限
            boolean senderRecallingOwnMessage = existingMessage.senderId() == accountId;
            boolean moderatorRecallingMemberMessage = operatorContext.isModerator()
                    && !channelContextApi.isModerator(existingMessage.channelId(), existingMessage.senderId());
            if (operatorContext.isMuted() || (!senderRecallingOwnMessage && !moderatorRecallingMemberMessage)) {
                throw ProblemException.of(MessageProblem.RECALL_FORBIDDEN);
            }
            // 撤销消息
            if (existingMessage.status() == CPMessageStatus.RECALLED) {
                return;
            }
            CPChannelMessage recalledMessage = new CPChannelMessage(
                    existingMessage.messageId(),
                    existingMessage.senderId(),
                    existingMessage.channelId(),
                    existingMessage.sequence(),
                    existingMessage.domain(),
                    Map.of(),
                    existingMessage.sendTime(),
                    List.of(),
                    "消息已撤回",
                    "",
                    CPMessageStatus.RECALLED
            );
            // 更新消息
            messageRepository.update(recalledMessage);
            // 删除提及
            mentionRepository.deleteByMessageId(existingMessage.messageId());
            // 撤回事件与正文清除在同一事务登记。
            messageEventNotifier.messageRecalled(recalledMessage);
        });
        AuditLogs.success("message.recall", accountId,
                "channel_id", channelId,
                "message_id", messageId);
    }
}
