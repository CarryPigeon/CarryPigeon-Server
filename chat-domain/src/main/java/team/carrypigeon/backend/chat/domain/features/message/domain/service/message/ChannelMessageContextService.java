package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAroundQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;

/**
 * 频道消息锚点上下文查询领域实现。
 * 职责：读取单条频道消息及锚点前后固定窗口。
 * 边界：不承载方向翻页、搜索、发送或撤回。
 */
@Service
public class ChannelMessageContextService implements ChannelMessageContextApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageContextService(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessageResult getMessage(long accountId, long channelId, long messageId) {
        channelContextApi.requireMemberContext(channelId, accountId);
        return ChannelMessageResult.of(requireChannelMessage(channelId, messageId));
    }

    @Override
    public ChannelMessageContext getMessagesAround(GetChannelMessagesAroundQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        CPChannelMessage anchorMessage = requireChannelMessage(channel.channelId(), query.anchorMessageId());

        Stream<CPChannelMessage> before = query.before() == 0
                ? Stream.empty()
                : messageRepository.findBeforeByChannelId(
                        channel.channelId(),
                        anchorMessage.sequence(),
                        query.before()
                ).stream();
        Stream<CPChannelMessage> after = query.after() == 0
                ? Stream.empty()
                : messageRepository.findAfterByChannelId(
                        channel.channelId(),
                        anchorMessage.sequence(),
                        query.after()
                ).stream();

        List<ChannelMessageResult> items = Stream.concat(
                        Stream.concat(before, Stream.of(anchorMessage)),
                        after
                )
                .sorted(Comparator.comparingLong(CPChannelMessage::sequence).reversed())
                .map(message -> ChannelMessageResult.of(message))
                .toList();
        return new ChannelMessageContext(items);
    }

    private CPChannelMessage requireChannelMessage(long channelId, long messageId) {
        CPChannelMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
        // 跨频道消息按不存在处理，避免向当前频道成员泄露其他频道的消息。
        if (message.channelId() != channelId) {
            throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
        }
        return message;
    }
}
