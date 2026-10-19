package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时连接索引。
 * 职责：维护账号与在线 Netty 通道的映射，并按账号广播已编码文本。
 * 边界：只管理连接与写入；不承载事件缓存、续传或任何业务规则。
 */
public class RealtimeConnectionIndex {

    private final ConcurrentHashMap<Long, Set<Channel>> channelsByAccountId = new ConcurrentHashMap<>();

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
     * @return 只读通道集合
     */
    public Set<Channel> channels(long accountId) {
        return Collections.unmodifiableSet(channelsByAccountId.getOrDefault(accountId, Set.of()));
    }

    /**
     * 向指定账号的在线通道广播已编码文本。
     * 约束：每个通道必须使用独立的帧实例，Netty 帧不可跨通道共享；失活或不可写通道在广播过程中被摘除。
     *
     * @param accountIds 目标账号
     * @param frameText 已编码的下行文本
     */
    public void broadcast(Collection<Long> accountIds, String frameText) {
        if (accountIds == null || accountIds.isEmpty() || frameText == null) {
            return;
        }
        for (Long accountId : accountIds) {
            if (accountId == null) {
                continue;
            }
            channels(accountId).forEach(channel -> {
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
}
