package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangeEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageChangeReader;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/** 从进程内事件缓冲读取消息变更，并转换为领域层使用的结果。 */
@Component
public class InMemoryMessageChangeReader implements MessageChangeReader {
    private final ChannelEventBuffer events;

    public InMemoryMessageChangeReader(ChannelEventBuffer events) {
        this.events = events;
    }

    @Override
    public MessageChangePage read(long channelId, MessageChangeCursor cursor, int limit) {
        try {
            ChannelEventBuffer.Cursor bufferCursor = cursor == null ? null : new ChannelEventBuffer.Cursor(
                    cursor.instanceId(), cursor.channelId(), cursor.sequence(), cursor.issuedAt());
            ChannelEventBuffer.Page page = events.read(channelId, bufferCursor, limit);
            ChannelEventBuffer.Cursor next = page.nextCursor();
            return new MessageChangePage(page.events().stream()
                    .map(event -> new MessageChangeEvent(event.eventId(), event.channelId(), event.messageId(),
                            event.sequence(), event.type(), event.createdAt()))
                    .toList(), new MessageChangeCursor(next.instanceId(), next.channelId(), next.sequence(), next.issuedAt()),
                    page.hasMore());
        } catch (ChannelEventBuffer.ExpiredCursorException exception) {
            throw ProblemException.of(MessageProblem.EVENT_CURSOR_EXPIRED);
        }
    }
}
