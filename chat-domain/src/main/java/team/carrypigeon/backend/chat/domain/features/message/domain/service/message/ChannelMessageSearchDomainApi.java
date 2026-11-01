package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageSearchApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;

/**
 * 频道消息搜索领域实现。
 * 职责：执行频道内关键字搜索并返回游标分页结果。
 * 边界：不承载时间线方向、锚点上下文、发送或撤回。
 */
@Service
public class ChannelMessageSearchDomainApi implements ChannelMessageSearchApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageSearchDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessagePage searchMessages(SearchChannelMessagesQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<CPChannelMessage> messages = messageRepository.searchByChannelId(
                channel.channelId(),
                query.keyword(),
                query.cursorMessageId(),
                query.senderAccountId(),
                query.domain(),
                query.limit() + 1
        );
        boolean hasMore = messages.size() > query.limit();
        List<CPChannelMessage> pageMessages = hasMore ? messages.subList(0, query.limit()) : messages;
        List<ChannelMessageResult> items = pageMessages.stream()
                .map(message -> new ChannelMessageResult(message.messageId()))
                .toList();
        Long nextCursor = hasMore ? items.getLast().messageId() : null;
        return new ChannelMessagePage(items, nextCursor, hasMore);
    }
}
