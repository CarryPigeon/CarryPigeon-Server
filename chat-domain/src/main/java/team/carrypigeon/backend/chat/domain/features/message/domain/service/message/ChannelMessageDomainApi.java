package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageDispatcher;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPMessageStatus;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：编排消息发送、撤回、持久化与提交后通知。
 * 边界：不承载请求字段校验；只处理频道权限、禁言、domain 支持、消息状态和事务。
 */
@Service
public class ChannelMessageDomainApi implements ChannelMessageApi {

    private static final String MENTION_TARGET_TYPE_USER = "user";

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final MessageEventNotifier messageEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;
    private final CPChannelMessageDispatcher channelMessageDispatcher;

    public ChannelMessageDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageEventNotifier messageEventNotifier,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
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
        long messageId = transactionRunner.runInTransaction(afterCommit -> {
            // 拉取上下文
            ChannelContext channelContext = channelContextApi.requireMemberContext(channelId, accountId);
            // 检查权限
            if (channelContext.isMuted()) {
                throw ProblemException.of(MessageProblem.USER_MUTED);
            }
            // 存在性校验
            if (!channelMessageDispatcher.supports(domain)) {
                throw ProblemException.of(MessageProblem.UNSUPPORTED_DOMAIN);
            }
            // 成员快照同时用于提及目标过滤和消息广播接收人，避免重复读取
            List<Long> memberAccountIds = channelContextApi.memberAccountIds(channelId);
            Set<Long> memberAccountIdSet = new HashSet<>(memberAccountIds);
            List<Long> normalizedMentions = mentions == null
                    ? List.of()
                    : mentions.stream()
                            .filter(memberAccountIdSet::contains)
                            .toList();
            // 组装消息
            CPChannelMessage message = new CPChannelMessage(
                    idGenerator.nextLongId(),
                    accountId,
                    channelId,
                    domain,
                    data,
                    timeProvider.nowInstant(),
                    normalizedMentions,
                    "",
                    CPMessageStatus.SENT
            );
            // 处理消息
            CPChannelMessageHandler handler = channelMessageDispatcher.dispatch(domain);
            // 持久化消息
            CPChannelMessage saved = messageRepository.save(handler.handle(message));
            // 生成提及记录；只保留当前频道成员，避免产生无法访问的提及
            List<Mention> savedMentions = saved.mentions().stream()
                    .map(targetAccountId -> new Mention(
                            idGenerator.nextLongId(),
                            channelId,
                            saved.messageId(),
                            accountId,
                            MENTION_TARGET_TYPE_USER,
                            targetAccountId,
                            saved.sendTime(),
                            false
                    ))
                    .toList();
            if (!savedMentions.isEmpty()) {
                mentionRepository.saveAll(savedMentions);
            }
            // 通知所有人
            messageEventNotifier.messageCreated(
                    afterCommit,
                    saved,
                    memberAccountIds
            );
            // 提及事件只广播给被提及账号
            savedMentions.forEach(mention -> messageEventNotifier.mentionCreated(afterCommit, mention));
            return saved.messageId();
        });
        AuditLogs.success("message.send", accountId,
                "channel_id", channelId,
                "message_id", messageId,
                "domain", domain);
        return new ChannelMessageResult(messageId);
    }

    @Override
    public void recallChannelMessage(long accountId, long channelId, long messageId) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 查询已有的消息
            CPChannelMessage existingMessage = messageRepository.findById(messageId)
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            // 验证消息属于该频道
            if (existingMessage.channelId() != channelId) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            // 验证操作权限
            ChannelContext operatorContext = channelContextApi.requireMemberContext(
                    existingMessage.channelId(),
                    accountId
            );
            boolean senderRecallingOwnMessage = existingMessage.senderId() == accountId;
            boolean moderatorRecallingMemberMessage = operatorContext.isModerator()
                    && !channelContextApi.moderatorAccounts(existingMessage.channelId()).contains(existingMessage.senderId());
            if (operatorContext.isMuted() || (!senderRecallingOwnMessage && !moderatorRecallingMemberMessage)) {
                throw ProblemException.of(MessageProblem.RECALL_FORBIDDEN);
            }
            // 撤销消息
            if (existingMessage.status() == CPMessageStatus.RECALLED) {
                return;
            }
            // 通知所有人
            List<Long> recipients = channelContextApi.memberAccountIds(existingMessage.channelId());
            CPChannelMessage recalledMessage = new CPChannelMessage(
                    existingMessage.messageId(),
                    existingMessage.senderId(),
                    existingMessage.channelId(),
                    existingMessage.domain(),
                    Map.of(),
                    existingMessage.sendTime(),
                    List.of(),
                    "消息已撤回",
                    CPMessageStatus.RECALLED
            );
            // 更新消息
            messageRepository.update(recalledMessage);
            // 删除提及
            mentionRepository.deleteByMessageId(existingMessage.messageId());
            // 通知所有人
            messageEventNotifier.messageRecalled(afterCommit, recalledMessage, recipients);
        });
        AuditLogs.success("message.recall", accountId,
                "channel_id", channelId,
                "message_id", messageId);
    }
}
