package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageHistoryApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.ListChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;

/**
 * 校验频道成员身份并按消息 ID 降序读取频道历史。
 */
@Service
public class ChannelMessageHistoryService implements ChannelMessageHistoryApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageHistoryService(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessagePage listMessages(long accountId, ListChannelMessagesQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), accountId);
        // 多取一条判断是否还有更旧的消息，避免为分页额外查询总数。
        int fetchLimit = query.limit() + 1;
        List<CPChannelMessage> messages = query.beforeMessageId() == null
                ? messageRepository.findLatestByChannelId(channel.channelId(), fetchLimit)
                : messageRepository.findBeforeByChannelId(channel.channelId(), query.beforeMessageId(), fetchLimit);
        boolean hasMore = messages.size() > query.limit();
        List<ChannelMessageResult> items = messages.stream()
                .limit(query.limit())
                .map(ChannelMessageResult::of)
                .toList();
        return new ChannelMessagePage(items, hasMore);
    }
}
