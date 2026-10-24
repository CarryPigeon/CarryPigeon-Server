package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageTimelineApi;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 频道消息时间线领域 API 实现。
 * 职责：直接承载频道消息历史读取与搜索用例。
 * 边界：不承载消息发布、编辑、撤回、删除、附件上传和置顶能力。
 */
@Service
public class ChannelMessageTimelineDomainApi implements ChannelMessageTimelineApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageTimelineDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query) {
        // 约束请求参数是否合理
        validateHistoryQuery(query);
        // 权限校验，判断是否是成员
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());

        if (query.aroundMessageId() != null) {
            CPChannelMessage targetMessage = requireMessage(query.aroundMessageId());
            if (targetMessage.channelId() != channel.channelId()) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            int beforeCount = query.before() == null ? 25 : query.before();
            int afterCount = query.after() == null ? 25 : query.after();
            List<ChannelMessageResult> messages = Stream.concat(
                            Stream.concat(
                                    messageRepository.findByChannelIdBefore(channel.channelId(), query.aroundMessageId(), beforeCount).stream(),
                                    Stream.of(targetMessage)
                            ),
                            messageRepository.findByChannelIdAfter(channel.channelId(), query.aroundMessageId(), afterCount).stream()
                    )
                    .sorted(Comparator.comparingLong(CPChannelMessage::messageId).reversed())
                    .map(ChannelMessageResult::from)
                    .toList();
            return new ChannelMessageHistoryResult(messages, null);
        }
        List<ChannelMessageResult> messages = messageRepository.findByChannelIdBefore(
                        channel.channelId(),
                        query.cursorMessageId(),
                        query.limit() + 1
                ).stream()
                .map(ChannelMessageResult::from)
                .toList();
        boolean hasMore = messages.size() > query.limit();
        List<ChannelMessageResult> pageItems = hasMore ? messages.subList(0, query.limit()) : messages;
        Long nextCursor = hasMore ? pageItems.get(pageItems.size() - 1).messageId() : null;
        return new ChannelMessageHistoryResult(pageItems, nextCursor);
    }

    @Override
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        validateSearchQuery(query);
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.searchByChannelId(
                        channel.channelId(),
                        query.keyword().trim(),
                        query.cursorMessageId(),
                        query.senderAccountId(),
                        normalizeDomain(query.domain()),
                        query.beforeMessageId(),
                        query.afterMessageId(),
                        query.limit() + 1
                ).stream()
                .map(ChannelMessageResult::from)
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
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "around_mid must be greater than 0");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "cursorMessageId must be greater than 0");
        }
        if (query.aroundMessageId() != null && query.cursorMessageId() != null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "cursor and around_mid cannot be used together");
        }
        if (query.before() != null && (query.before() < 0 || query.before() > 50)) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "before must be between 0 and 50");
        }
        if (query.after() != null && (query.after() < 0 || query.after() > 50)) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "after must be between 0 and 50");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "limit must be between 1 and 50");
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
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "keyword must not be blank");
        }
        if (query.keyword().trim().length() > 100) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "keyword length must be between 1 and 100");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.of(MessageProblem.CURSOR_INVALID);
        }
        if (query.senderAccountId() != null && query.senderAccountId() <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "sender_uid must be greater than 0");
        }
        if (query.beforeMessageId() != null && query.beforeMessageId() <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "before_mid must be greater than 0");
        }
        if (query.afterMessageId() != null && query.afterMessageId() <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "after_mid must be greater than 0");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "limit must be between 1 and 50");
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

    private CPChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, fieldName + " must be greater than 0");
        }
    }
}
