package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;

/**
 * 频道消息时间线领域 API 实现。
 * 职责：直接承载频道消息历史读取与搜索用例。
 * 边界：不承载请求字段校验、消息发布、编辑、撤回、删除、附件上传和置顶能力。
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
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());

        if (query.aroundMessageId() != null) {
            CPChannelMessage targetMessage = messageRepository.findById(query.aroundMessageId())
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
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
        Long nextCursor = hasMore ? pageItems.getLast().messageId() : null;
        return new ChannelMessageHistoryResult(pageItems, nextCursor);
    }

    @Override
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.searchByChannelId(
                        channel.channelId(),
                        query.keyword(),
                        query.cursorMessageId(),
                        query.senderAccountId(),
                        query.domain(),
                        query.beforeMessageId(),
                        query.afterMessageId(),
                        query.limit() + 1
                ).stream()
                .map(ChannelMessageResult::from)
                .toList();
        return new ChannelMessageSearchResult(messages);
    }
}
