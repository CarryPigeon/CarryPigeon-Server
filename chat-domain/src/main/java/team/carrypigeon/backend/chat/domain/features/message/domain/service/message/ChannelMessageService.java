package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
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

    public ChannelMessageService(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageEventNotifier messageEventNotifier,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            CPChannelMessageDispatcher channelMessageDispatcher
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.messageEventNotifier = messageEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.channelMessageDispatcher = channelMessageDispatcher;
    }

    @Override
    public ChannelMessageResult sendChannelMessage(long accountId, long channelId, String domain, Map<String, Object> data, List<Long> mentions) {
        // 提及在业务上是集合；缺省与空列表、元素顺序和重复项不形成不同的发送请求。
        List<Long> requestedMentions = mentions == null ? List.of()
                : mentions.stream().filter(Objects::nonNull).distinct().sorted().toList();
        CPChannelMessage message = transactionRunner.runInTransaction(afterCommit -> {
            // 拉取上下文
            ChannelContext channelContext = channelContextApi.requireMemberContextForUpdate(channelId, accountId);
            long proposedMessageId = idGenerator.nextLongId();
            // 检查权限
            if (channelContext.isMuted()) {
                throw ProblemException.of(MessageProblem.USER_MUTED);
            }
            // 存在性校验
            if (!channelMessageDispatcher.supports(domain)) {
                throw ProblemException.of(MessageProblem.UNSUPPORTED_DOMAIN);
            }
            // 组装消息
            CPChannelMessage originalMessage = new CPChannelMessage(
                    proposedMessageId,
                    accountId,
                    channelId,
                    domain,
                    data,
                    timeProvider.nowInstant(),
                    requestedMentions,
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
            // 插件处理后的提及才是最终候选。只为发送时仍在群内的账号创建收件箱记录并推送。
            List<Long> recipients = channelContextApi.findMemberAccountIds(channelId, finalMessage.mentions());
            List<Mention> savedMentions = recipients.stream()
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
            // 消息提交后才发布短期事件，避免事务回滚后客户端看到不存在的消息。
            messageEventNotifier.messageCreated(afterCommit, finalMessage);
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


    @Override
    public void recallChannelMessage(long accountId, long channelId, long messageId) {
        transactionRunner.runInTransaction(afterCommit -> {
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
            // 内容清除提交后再通知，短期缓冲不保存正文。
            messageEventNotifier.messageRecalled(afterCommit, recalledMessage);
        });
        AuditLogs.success("message.recall", accountId,
                "channel_id", channelId,
                "message_id", messageId);
    }
}
