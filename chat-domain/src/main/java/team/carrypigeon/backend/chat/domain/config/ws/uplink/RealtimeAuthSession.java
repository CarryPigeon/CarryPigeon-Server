package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import java.time.Instant;
import java.util.Map;

/**
 * 一次认证成功后的会话快照。
 * 职责：承载内建认证命令回复 `auth.ok` / `reauth.ok` 所需的稳定字段。
 * 边界：只有认证结论数据，不含连接对象、不含传输实现细节。
 *
 * @param accountId 账号 ID
 * @param expiresAt 访问令牌过期时间
 * @param serverId 服务端标识
 */
public record RealtimeAuthSession(long accountId, Instant expiresAt, String serverId) {

    /**
     * 认证成功应答载荷。
     * 约束：字段名为线上协议的一部分，客户端依赖其保持稳定。
     *
     * @return 应答载荷
     */
    public Map<String, Object> ackData() {
        return Map.of(
                "uid", Long.toString(accountId),
                "expires_at", expiresAt.toEpochMilli(),
                "server_id", serverId
        );
    }
}
