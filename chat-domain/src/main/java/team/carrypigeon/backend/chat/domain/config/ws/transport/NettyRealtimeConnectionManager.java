package team.carrypigeon.backend.chat.domain.config.ws.transport;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnectionManager;

/**
 * 实时连接索引。
 * 职责：维护账号与在线 Netty 通道的映射，并按账号广播已编码文本。
 * 边界：只管理连接与写入；不承载事件缓存、续传或任何业务规则。
 */
@Component
public class NettyRealtimeConnectionManager implements RealtimeConnectionManager {

    private final ConcurrentHashMap<Long, Set<RealtimeConnection>> channelsByAccountId = new ConcurrentHashMap<>();

    @Override
    public void register(long accountId, RealtimeConnection connection) {
        channelsByAccountId.computeIfAbsent(accountId, ignored -> ConcurrentHashMap.newKeySet()).add(connection);
    }

    @Override
    public void unregister(long accountId, RealtimeConnection connection) {
        Set<RealtimeConnection> connections = channelsByAccountId.get(accountId);
        if (connections == null) {
            return;
        }
        connections.remove(connection);
        if (connections.isEmpty()) {
            channelsByAccountId.remove(accountId, connections);
        }
    }

    @Override
    public Set<RealtimeConnection> channels(long accountId) {
        return Collections.unmodifiableSet(channelsByAccountId.getOrDefault(accountId, Set.of()));
    }

    @Override
    public void broadcast(Collection<Long> accountIds, String frameText) {
        if (accountIds == null || accountIds.isEmpty() || frameText == null) {
            return;
        }
        for (Long accountId : accountIds) {
            if (accountId == null) {
                continue;
            }
            channels(accountId).forEach(connection -> {
                if (!connection.isActive()) {
                    unregister(accountId, connection);
                    return;
                }
                if (!connection.isWritable()) {
                    unregister(accountId, connection);
                    connection.close();
                    return;
                }
                connection.send(frameText);
            });
        }
    }
}
