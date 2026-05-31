package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.channel.Channel;
import java.util.ArrayList;
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

    private static final int MAX_EVENTS = 200;

    private final ConcurrentHashMap<Long, Set<Channel>> channelsByAccountId = new ConcurrentHashMap<>();
    private final List<StoredRealtimeEvent> eventLog = Collections.synchronizedList(new ArrayList<>());

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

    public void appendEvent(StoredRealtimeEvent event) {
        eventLog.add(event);
        if (eventLog.size() > MAX_EVENTS) {
            eventLog.removeFirst();
        }
    }

    public List<StoredRealtimeEvent> eventsAfter(String lastEventId) {
        synchronized (eventLog) {
            if (lastEventId == null || lastEventId.isBlank()) {
                return List.of();
            }
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

    public record StoredRealtimeEvent(String eventId, String eventType, long serverTime, Object payload) {
    }
}
