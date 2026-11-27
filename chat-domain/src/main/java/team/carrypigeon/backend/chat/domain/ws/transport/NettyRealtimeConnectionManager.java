package team.carrypigeon.backend.chat.domain.ws.transport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeConnection;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeConnectionManager;

/**
 * 单实例实时连接索引，维护账号多端连接和频道到在线账号的反向映射。
 * 认证与成员集合变更时读取权威关系；频道广播仅校验在线候选，索引本身不授予成员权限。
 * 同一账号的连接、订阅更新在 compute 内串行化，避免末端断连与新连接竞争导致索引丢失。
 */
@Slf4j
@Component
public class NettyRealtimeConnectionManager implements RealtimeConnectionManager {

    private final ConcurrentHashMap<Long, OnlineAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> accountsByChannel = new ConcurrentHashMap<>();
    private final ChannelContextApi channelContext;

    public NettyRealtimeConnectionManager(ChannelContextApi channelContext) {
        this.channelContext = channelContext;
    }

    @Override
    public void register(long accountId, RealtimeConnection connection) {
        accounts.compute(accountId, (id, previous) -> {
            Set<Long> channelIds = Set.copyOf(channelContext.joinedChannelIds(id));
            Set<RealtimeConnection> connections = previous == null ? new HashSet<>() : previous.connections();
            connections.add(connection);
            updateSubscriptions(id, previous == null ? Set.of() : previous.channelIds(), channelIds);
            return new OnlineAccount(connections, channelIds);
        });
    }

    @Override
    public void unregister(long accountId, RealtimeConnection connection) {
        accounts.computeIfPresent(accountId, (id, account) -> {
            account.connections().remove(connection);
            if (account.connections().isEmpty()) {
                updateSubscriptions(id, account.channelIds(), Set.of());
                return null;
            }
            return account;
        });
    }

    @Override
    public Set<RealtimeConnection> channels(long accountId) {
        Set<RealtimeConnection> snapshot = new HashSet<>();
        accounts.computeIfPresent(accountId, (id, account) -> {
            snapshot.addAll(account.connections());
            return account;
        });
        return Set.copyOf(snapshot);
    }

    @Override
    public void refreshAccountChannels(Collection<Long> accountIds) {
        for (long accountId : accountIds) {
            accounts.computeIfPresent(accountId, (id, previous) -> {
                try {
                    Set<Long> channelIds = Set.copyOf(channelContext.joinedChannelIds(id));
                    updateSubscriptions(id, previous.channelIds(), channelIds);
                    return new OnlineAccount(previous.connections(), channelIds);
                } catch (RuntimeException exception) {
                    // 提交已经成功；刷新失败时摘除并关闭该账号连接，重新认证后再建订阅。
                    // 不能把异常回传为治理失败，也不能继续信任未刷新的权限索引。
                    updateSubscriptions(id, previous.channelIds(), Set.of());
                    previous.connections().forEach(RealtimeConnection::close);
                    log.atError()
                        .addKeyValue("target_uid", id)
                        .setCause(exception)
                        .log("实时订阅刷新失败，连接已关闭");
                    return null;
                }
            });
        }
    }

    /**
     * 频道消息仅枚举本机在线候选，并在实际投递前从数据库过滤成员与已撤下频道。
     * 授权查询开始前已提交的撤权不会收到新帧；查询时仍有效的在途帧可能先于撤权入队，无法撤销已排队数据。
     */
    public void broadcastToChannel(long channelId, String frameText) {
        Set<Long> online = accountsByChannel.get(channelId);
        if (online == null || online.isEmpty()) {
            return;
        }
        for (long accountId : online) {
            sendToAccount(accountId, channelId, frameText);
        }
    }

    @Override
    public void broadcast(Collection<Long> accountIds, String frameText) {
        if (accountIds == null || frameText == null) {
            return;
        }
        for (Long accountId : accountIds) {
            if (accountId != null) {
                sendToAccount(accountId, null, frameText);
            }
        }
    }

    private void sendToAccount(long accountId, Long channelId, String frameText) {
        accounts.computeIfPresent(accountId, (id, account) -> {
            // 授权查询后账号可能断线或刷新订阅；新的连接状态不能沿用过期订阅。
            if (channelId != null && !account.channelIds().contains(channelId)) {
                return account;
            }
            account.connections().removeIf(connection -> {
                if (!connection.isActive()) {
                    return true;
                }
                if (!connection.isWritable()) {
                    connection.close();
                    log.atWarn()
                        .addKeyValue("target_uid", id)
                        .log("实时连接不可写，已关闭");
                    return true;
                }
                try {
                    connection.send(frameText);
                    return false;
                } catch (RuntimeException exception) {
                    connection.close();
                    log.atWarn()
                        .addKeyValue("target_uid", id)
                        .setCause(exception)
                        .log("实时帧提交失败，连接已关闭");
                    return true;
                }
            });
            if (account.connections().isEmpty()) {
                updateSubscriptions(id, account.channelIds(), Set.of());
                return null;
            }
            return account;
        });
    }

    /** 仅在对应账号的 compute 内调用；频道集合也以 compute 原子增删，避免空集合移除竞争。 */
    private void updateSubscriptions(long accountId, Set<Long> previous, Set<Long> current) {
        for (long channelId : previous) {
            if (!current.contains(channelId)) {
                accountsByChannel.computeIfPresent(channelId, (id, accountIds) -> {
                    accountIds.remove(accountId);
                    return accountIds.isEmpty() ? null : accountIds;
                });
            }
        }
        for (long channelId : current) {
            if (!previous.contains(channelId)) {
                accountsByChannel.compute(channelId, (id, accountIds) -> {
                    Set<Long> updated = accountIds == null ? ConcurrentHashMap.newKeySet() : accountIds;
                    updated.add(accountId);
                    return updated;
                });
            }
        }
    }

    /** 连接集合只在账号 compute 内访问；频道集合为不可变快照。 */
    private record OnlineAccount(Set<RealtimeConnection> connections, Set<Long> channelIds) { }
}
