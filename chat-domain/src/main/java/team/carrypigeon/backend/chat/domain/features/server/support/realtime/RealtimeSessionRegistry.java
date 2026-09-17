package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时会话注册表。
 * 职责：维护当前在线账户与 Netty 通道之间的最小映射关系。
 * 边界：只管理会话索引，不承载消息业务规则。
 */
public class RealtimeSessionRegistry {

    private static final int MAX_EVENTS_PER_ACCOUNT = 1000;

    private final ConcurrentHashMap<Long, Set<Channel>> channelsByAccountId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AccountEventLog> eventLogsByAccountId = new ConcurrentHashMap<>();
    private final TimeProviderImpl timeProvider;
    private final long eventRetentionMillis;
    private final int maxEventAccounts;

    /**
     * 创建使用生产默认资源边界的注册表。
     * 边界：主要供轻量测试和独立构造使用，生产装配应传入统一 TimeProvider 与显式配置。
     */
    public RealtimeSessionRegistry() {
        this(new TimeProviderImpl(Clock.systemUTC()), Duration.ofHours(1), 10_000);
    }

    public RealtimeSessionRegistry(TimeProviderImpl timeProvider, Duration eventRetention, int maxEventAccounts) {
        if (timeProvider == null) {
            throw new IllegalArgumentException("timeProvider must not be null");
        }
        if (eventRetention == null || eventRetention.isZero() || eventRetention.isNegative()) {
            throw new IllegalArgumentException("eventRetention must be positive");
        }
        if (maxEventAccounts <= 0) {
            throw new IllegalArgumentException("maxEventAccounts must be greater than 0");
        }
        this.timeProvider = timeProvider;
        this.eventRetentionMillis = eventRetention.toMillis();
        this.maxEventAccounts = maxEventAccounts;
    }

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
     * 向指定账户的当前在线通道广播已序列化文本。
     * 传输对象由 realtime support 层创建，避免领域发布器直接依赖 Netty。
     */
    public void writeText(Collection<Long> accountIds, String frameText) {
        if (accountIds == null || frameText == null) {
            return;
        }
        for (Long accountId : accountIds) {
            if (accountId == null) {
                continue;
            }
            getChannels(accountId).forEach(channel -> {
                if (!channel.isActive()) {
                    unregister(accountId, channel);
                    return;
                }
                if (!channel.isWritable()) {
                    unregister(accountId, channel);
                    channel.close();
                    return;
                }
                channel.writeAndFlush(new TextWebSocketFrame(frameText));
            });
        }
    }

    /**
     * 追加一条可用于断线续传的实时事件。
     * 输入：已完成序列化边界控制的事件快照。
     * 副作用：写入内存事件日志；超过窗口上限时丢弃最旧事件。
     *
     * @param event 实时事件快照
     */
    public void appendEvent(StoredRealtimeEvent event) {
        long nowMillis = timeProvider.nowMillis();
        for (Long recipientAccountId : event.recipientAccountIds()) {
            AccountEventLog eventLog = eventLogsByAccountId.computeIfAbsent(
                    recipientAccountId,
                    ignored -> new AccountEventLog()
            );
            synchronized (eventLog) {
                removeExpired(eventLog, nowMillis);
                if (eventLog.events.size() == MAX_EVENTS_PER_ACCOUNT) {
                    eventLog.events.removeFirst();
                }
                eventLog.events.addLast(new LoggedEvent(event, nowMillis));
                eventLog.lastWriteMillis = nowMillis;
            }
        }
        enforceAccountLimit();
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
        AccountEventLog eventLog = eventLogsByAccountId.get(accountId);
        if (eventLog == null) {
            return null;
        }
        synchronized (eventLog) {
            removeExpired(eventLog, timeProvider.nowMillis());
            if (eventLog.events.isEmpty()) {
                eventLogsByAccountId.remove(accountId, eventLog);
                return null;
            }
            boolean anchorFound = false;
            List<StoredRealtimeEvent> eventsAfterAnchor = new ArrayList<>();
            for (LoggedEvent loggedEvent : eventLog.events) {
                StoredRealtimeEvent event = loggedEvent.event();
                if (anchorFound) {
                    eventsAfterAnchor.add(event);
                } else if (event.eventId().equals(lastEventId)) {
                    anchorFound = true;
                }
            }
            if (!anchorFound) {
                return null;
            }
            return List.copyOf(eventsAfterAnchor);
        }
    }

    private void removeExpired(AccountEventLog eventLog, long nowMillis) {
        long cutoff = nowMillis - eventRetentionMillis;
        while (!eventLog.events.isEmpty() && eventLog.events.getFirst().storedAtMillis() <= cutoff) {
            eventLog.events.removeFirst();
        }
    }

    /**
     * 把账号事件窗口数量限制在配置上限内。
     * 淘汰语义：优先移除最后写入时间最早的账号，客户端随后按既有完整同步语义恢复。
     */
    private void enforceAccountLimit() {
        synchronized (eventLogsByAccountId) {
            while (eventLogsByAccountId.size() > maxEventAccounts) {
                Long oldestAccountId = eventLogsByAccountId.entrySet().stream()
                        .min((left, right) -> {
                            int timeComparison = Long.compare(
                                    left.getValue().lastWriteMillis,
                                    right.getValue().lastWriteMillis
                            );
                            return timeComparison != 0
                                    ? timeComparison
                                    : Long.compare(left.getKey(), right.getKey());
                        })
                        .map(java.util.Map.Entry::getKey)
                        .orElse(null);
                if (oldestAccountId == null) {
                    return;
                }
                eventLogsByAccountId.remove(oldestAccountId);
            }
        }
    }

    private static final class AccountEventLog {

        private final ArrayDeque<LoggedEvent> events = new ArrayDeque<>(MAX_EVENTS_PER_ACCOUNT);
        private volatile long lastWriteMillis;
    }

    private record LoggedEvent(StoredRealtimeEvent event, long storedAtMillis) {
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
