package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息时间线领域 API 实现。
 * 职责：直接承载频道消息历史读取与搜索用例。
 * 边界：不承载消息发布、编辑、撤回、删除、附件上传和置顶能力。
 */
@Service
public class ChannelMessageTimelineDomainApi extends AbstractMessageDomainSupport implements ChannelMessageTimelineApi {

    public ChannelMessageTimelineDomainApi(
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
    public ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query) {
        validateHistoryQuery(query);
        ChannelMessagingContext channel = channelMessagingApi.requireMemberChannel(query.channelId(), query.accountId());
        if (query.aroundMessageId() != null) {
            ChannelMessage targetMessage = requireMessage(query.aroundMessageId());
            if (targetMessage.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            int beforeCount = query.before() == null ? 25 : query.before();
            int afterCount = query.after() == null ? 25 : query.after();
            List<ChannelMessageResult> messages = Stream.concat(
                            Stream.concat(
                                    messageRepository.findByChannelIdBefore(channel.id(), query.aroundMessageId(), beforeCount).stream(),
                                    Stream.of(targetMessage)
                            ),
                            messageRepository.findByChannelIdAfter(channel.id(), query.aroundMessageId(), afterCount).stream()
                    )
                    .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
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

    @Override
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        validateSearchQuery(query);
        ChannelMessagingContext channel = channelMessagingApi.requireMemberChannel(query.channelId(), query.accountId());
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

    /**
     * 校验频道消息历史查询参数。
     * 约束：cursor 与 around_mid 不能同时使用，前后窗口和 limit 都限制在 50 以内。
     *
     * @param query 历史消息查询
     */
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

    /**
     * 校验频道消息搜索查询参数。
     * 约束：关键词必填且不超过 100 字符，消息边界、发送者和 cursor 必须为正数。
     *
     * @param query 消息搜索查询
     */
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

    /**
     * 规范化客户端 domain 过滤值。
     *
     * @param domain 客户端提交的 domain 过滤值
     * @return 领域消息类型过滤值，缺失时为 null
     */
    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return domain.trim();
    }
}
