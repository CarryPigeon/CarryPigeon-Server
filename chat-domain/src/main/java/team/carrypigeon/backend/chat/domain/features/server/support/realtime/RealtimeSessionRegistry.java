package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时会话注册表。
 * 职责：维护当前在线账户与 Netty 通道之间的最小映射关系。
 * 边界：只管理会话索引，不承载消息业务规则。
 */
public class RealtimeSessionRegistry {

    private static final int MAX_EVENTS_PER_ACCOUNT = 1000;

    private final ConcurrentHashMap<Long, Set<Channel>> channelsByAccountId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<StoredRealtimeEvent>> eventLogsByAccountId = new ConcurrentHashMap<>();

    /**
     * 注册账户实时通道。
     *
     * @param accountId 账户 ID
     * @param channel Netty 通道
     */
    public void register(long accountId, Channel channel) {
        channelsByAccountId.computeIfAbsent(accountId, ignored -> ConcurrentHashMap.newKeySet()).add(channel);
    }

    /**
     * 移除账户实时通道。
     *
     * @param accountId 账户 ID
     * @param channel Netty 通道
     */
    public void unregister(long accountId, Channel channel) {
        Set<Channel> channels = channelsByAccountId.get(accountId);
        if (channels == null) {
            return;
        }
        channels.remove(channel);
        if (channels.isEmpty()) {
            channelsByAccountId.remove(accountId, channels);
        }
    }

    /**
     * 读取账户当前在线通道。
     *
     * @param accountId 账户 ID
     * @return 当前在线通道集合
     */
    public Set<Channel> getChannels(long accountId) {
        return Collections.unmodifiableSet(channelsByAccountId.getOrDefault(accountId, Set.of()));
    }

    /**
     * 追加一条可用于断线续传的实时事件。
     * 输入：已完成序列化边界控制的事件快照。
     * 副作用：写入内存事件日志；超过窗口上限时丢弃最旧事件。
     *
     * @param event 实时事件快照
     */
    public void appendEvent(StoredRealtimeEvent event) {
        StoredRealtimeEvent storedEvent = event.withRecipients();
        for (Long recipientAccountId : storedEvent.recipientAccountIds()) {
            List<StoredRealtimeEvent> eventLog = eventLogsByAccountId.computeIfAbsent(
                    recipientAccountId,
                    ignored -> Collections.synchronizedList(new ArrayList<>())
            );
            synchronized (eventLog) {
                eventLog.add(storedEvent);
                if (eventLog.size() > MAX_EVENTS_PER_ACCOUNT) {
                    eventLog.removeFirst();
                }
            }
        }
    }

    /**
     * 查询指定事件之后仍保留在窗口内的事件列表。
     * 输入：客户端最后已确认的事件 ID。
     * 输出：找到锚点时返回后续事件；若锚点已过期则返回 null 表示无法续传。
     *
     * @param lastEventId 客户端最后已确认的事件 ID
     * @return 后续事件列表；为空表示没有新事件；null 表示事件过旧
     */
    public List<StoredRealtimeEvent> eventsAfter(long accountId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return List.of();
        }
        List<StoredRealtimeEvent> eventLog = eventLogsByAccountId.get(accountId);
        if (eventLog == null) {
            return null;
        }
        synchronized (eventLog) {
            int index = -1;
            for (int cursor = 0; cursor < eventLog.size(); cursor++) {
                if (eventLog.get(cursor).eventId().equals(lastEventId)) {
                    index = cursor;
                    break;
                }
            }
            if (index < 0) {
                return null;
            }
            return List.copyOf(eventLog.subList(index + 1, eventLog.size()));
        }
    }

    /**
     * 已缓存的 realtime 事件。
     * 职责：用于连接恢复时按 eventId 回放离线期间的事件。
     */
    public record StoredRealtimeEvent(
            String eventId,
            String eventType,
            long serverTime,
            Object payload,
            Set<Long> recipientAccountIds
    ) {

        public StoredRealtimeEvent {
            recipientAccountIds = recipientAccountIds == null ? Set.of() : Set.copyOf(recipientAccountIds);
        }

        public StoredRealtimeEvent withRecipients() {
            return new StoredRealtimeEvent(eventId, eventType, serverTime, payload, recipientAccountIds);
        }
    }

    public static StoredRealtimeEvent event(
            String eventId,
            String eventType,
            long serverTime,
            Object payload,
            Collection<Long> recipientAccountIds
    ) {
        return new StoredRealtimeEvent(
                eventId,
                eventType,
                serverTime,
                payload,
                recipientAccountIds == null ? Set.of() : Set.copyOf(recipientAccountIds)
        );
    }
}
