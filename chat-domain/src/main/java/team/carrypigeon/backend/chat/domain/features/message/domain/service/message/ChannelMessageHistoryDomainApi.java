package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageHistoryApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAfterQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesBeforeQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetLatestChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;

/**
 * 频道消息方向查询领域实现。
 * 职责：分别实现最新、更旧和更新三个固定方向的消息查询。
 * 边界：不承载锚点上下文、搜索、发送或撤回。
 */
@Service
public class ChannelMessageHistoryDomainApi implements ChannelMessageHistoryApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageHistoryDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessagePage getLatestMessages(GetLatestChannelMessagesQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<CPChannelMessage> messages = messageRepository.findLatestByChannelId(
                channel.channelId(),
                query.limit() + 1
        );
        return toPage(messages, query.limit());
    }

    @Override
    public ChannelMessagePage getMessagesBefore(GetChannelMessagesBeforeQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<CPChannelMessage> messages = messageRepository.findBeforeByChannelId(
                channel.channelId(),
                query.cursorMessageId(),
                query.limit() + 1
        );
        return toPage(messages, query.limit());
    }

    @Override
    public ChannelMessagePage getMessagesAfter(GetChannelMessagesAfterQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        List<CPChannelMessage> messages = messageRepository.findAfterByChannelId(
                channel.channelId(),
                query.cursorMessageId(),
                query.limit() + 1
        );
        return toPage(messages, query.limit());
    }

    /**
     * 把仓储返回的探测结果裁剪为固定页大小。
     * 约束：after 查询的仓储结果按消息 ID 升序，latest/before 按降序；本方法只做数量裁剪，不改变方向。
     */
    private ChannelMessagePage toPage(List<CPChannelMessage> messages, int limit) {
        boolean hasMore = messages.size() > limit;
        List<CPChannelMessage> pageMessages = hasMore ? messages.subList(0, limit) : messages;
        List<ChannelMessageResult> items = pageMessages.stream()
                .map(message -> new ChannelMessageResult(message.messageId()))
                .toList();
        Long nextCursor = hasMore ? items.getLast().messageId() : null;
        return new ChannelMessagePage(items, nextCursor, hasMore);
    }
}
