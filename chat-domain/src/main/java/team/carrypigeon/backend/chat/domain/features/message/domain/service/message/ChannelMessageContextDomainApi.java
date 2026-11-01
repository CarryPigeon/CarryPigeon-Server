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
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;

/**
 * 频道消息锚点上下文查询领域实现。
 * 职责：读取锚点消息及其前后固定窗口。
 * 边界：不承载方向翻页、搜索、发送或撤回。
 */
@Service
public class ChannelMessageContextDomainApi implements ChannelMessageContextApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;

    public ChannelMessageContextDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChannelMessageContext getMessagesAround(GetChannelMessagesAroundQuery query) {
        ChannelContext channel = channelContextApi.requireMemberContext(query.channelId(), query.accountId());
        CPChannelMessage anchorMessage = messageRepository.findById(query.anchorMessageId())
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
        if (anchorMessage.channelId() != channel.channelId()) {
            throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
        }

        Stream<CPChannelMessage> before = query.before() == 0
                ? Stream.empty()
                : messageRepository.findBeforeByChannelId(
                        channel.channelId(),
                        query.anchorMessageId(),
                        query.before()
                ).stream();
        Stream<CPChannelMessage> after = query.after() == 0
                ? Stream.empty()
                : messageRepository.findAfterByChannelId(
                        channel.channelId(),
                        query.anchorMessageId(),
                        query.after()
                ).stream();

        List<ChannelMessageResult> items = Stream.concat(
                        Stream.concat(before, Stream.of(anchorMessage)),
                        after
                )
                .sorted(Comparator.comparingLong(CPChannelMessage::messageId).reversed())
                .map(message -> new ChannelMessageResult(message.messageId()))
                .toList();
        return new ChannelMessageContext(items);
    }
}
