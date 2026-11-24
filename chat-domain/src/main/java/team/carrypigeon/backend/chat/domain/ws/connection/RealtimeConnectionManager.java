package team.carrypigeon.backend.chat.domain.ws.connection;

import java.util.Collection;
import java.util.Set;

/**
 * 实时通道的管理
 * 职责：提供实时通道与用户之间的映射
 * */
public interface RealtimeConnectionManager {

    /** 提交后刷新这些账号的频道订阅；只处理本机在线账号，不读取全频道成员。 */
    void refreshAccountChannels(Collection<Long> accountIds);

    /**
     * 注册账户实时通道。
     *
     * @param accountId 账户 ID
     * @param connection Netty 通道
     */
    void register(long accountId, RealtimeConnection connection);

    /**
     * 移除账户实时通道。
     *
     * @param accountId 账户 ID
     * @param connection Netty 通道
     */
    void unregister(long accountId, RealtimeConnection connection);

    /**
     * 读取账户当前在线通道。
     *
     * @param accountId 账户 ID
     * @return 只读通道集合
     */
    Set<RealtimeConnection> channels(long accountId);

    /**
     * 向指定账号的在线通道广播已编码文本。
     * 约束：每个通道必须使用独立的帧实例，Netty 帧不可跨通道共享；失活或不可写通道在广播过程中被摘除。
     *
     * @param accountIds 目标账号
     * @param frameText 已编码的下行文本
     */
    void broadcast(Collection<Long> accountIds, String frameText);
}
