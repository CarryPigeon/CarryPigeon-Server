package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道事件的进程内短期恢复缓冲区。
 * 只接收频道成员均可读取的数据；调用方必须在业务事务提交后发布事件。
 */
@Component
public class ChannelEventBuffer {
    private static final long RETENTION_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final int MAX_EVENTS = 10_000;
    private final UUID instanceId = UUID.randomUUID();
    private final TimeProvider time;
    private final ArrayDeque<BufferedEvent> events = new ArrayDeque<>();
    private final Map<Long, Discarded> discardedThrough = new LinkedHashMap<>();
    private long sequence;
    private long globalDiscardedThrough;

    public ChannelEventBuffer(TimeProvider time) {
        this.time = time;
    }

    /** 事件内容只包含频道及消息 ID；同一锁保证序号、插入和清理顺序一致。 */
    public synchronized BufferedEvent append(long channelId, long eventId, String type, long messageId) {
        long now = time.nowMillis();
        prune(now);
        BufferedEvent event = new BufferedEvent(++sequence, channelId, eventId, type, messageId, now);
        events.addLast(event);
        while (events.size() > MAX_EVENTS) {
            discard(events.removeFirst());
        }
        return event;
    }

    /** 游标为空时记录当前最新事件位置；后续按频道读取该位置之后的事件，超时或发生截断时要求重新读取快照。 */
    public synchronized Page read(long channelId, Cursor cursor, int limit) {
        long now = time.nowMillis();
        prune(now);
        if (cursor == null) {
            return new Page(List.of(), new Cursor(instanceId, channelId, sequence, now), false, sequence, discardedThrough.getOrDefault(channelId, new Discarded(0, 0)).sequence());
        }
        if (!instanceId.equals(cursor.instanceId()) || cursor.channelId() != channelId
                || cursor.sequence() < 0 || cursor.sequence() > sequence
                || cursor.issuedAt() > now || now - cursor.issuedAt() >= RETENTION_MILLIS
                || cursor.sequence() < globalDiscardedThrough
                || cursor.sequence() < discardedThrough.getOrDefault(channelId, new Discarded(0, 0)).sequence()) {
            throw new ExpiredCursorException();
        }
        List<BufferedEvent> found = new ArrayList<>();
        for (BufferedEvent event : events) {
            if (event.channelId() == channelId && event.sequence() > cursor.sequence()) {
                found.add(event);
                if (found.size() > limit) break;
            }
        }
        boolean hasMore = found.size() > limit;
        List<BufferedEvent> items = hasMore ? List.copyOf(found.subList(0, limit)) : List.copyOf(found);
        long next = hasMore ? items.getLast().sequence() : sequence;
        // 分页尚未读完时不能延长原游标的有效期。
        long issuedAt = hasMore ? cursor.issuedAt() : now;
        return new Page(items, new Cursor(instanceId, channelId, next, issuedAt), hasMore, sequence, discardedThrough.getOrDefault(channelId, new Discarded(0, 0)).sequence());
    }

    private void prune(long now) {
        while (!events.isEmpty() && now - events.getFirst().createdAt() >= RETENTION_MILLIS) {
            discard(events.removeFirst());
        }
        // 有效游标至多保留两分钟；缓冲为空后旧频道的截断标记可由游标时间判断。
        discardedThrough.values().removeIf(discarded -> now - discarded.discardedAt() >= RETENTION_MILLIS);
    }

    private void discard(BufferedEvent event) {
        discardedThrough.remove(event.channelId());
        discardedThrough.put(event.channelId(), new Discarded(event.sequence(), time.nowMillis()));
        if (discardedThrough.size() > MAX_EVENTS) {
            var oldest = discardedThrough.entrySet().iterator().next();
            globalDiscardedThrough = Math.max(globalDiscardedThrough, oldest.getValue().sequence());
            discardedThrough.remove(oldest.getKey());
        }
    }

    private record Discarded(long sequence, long discardedAt) { }
    public record BufferedEvent(long sequence, long channelId, long eventId, String type, long messageId, long createdAt) { }
    public record Cursor(UUID instanceId, long channelId, long sequence, long issuedAt) { }
    public record Page(List<BufferedEvent> events, Cursor nextCursor, boolean hasMore,
            long latestSequence, long expiredThrough) { }
    public static final class ExpiredCursorException extends RuntimeException { }
}
