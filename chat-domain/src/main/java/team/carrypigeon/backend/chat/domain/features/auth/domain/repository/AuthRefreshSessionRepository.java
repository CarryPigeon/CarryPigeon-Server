package team.carrypigeon.backend.chat.domain.features.auth.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;

/**
 * 刷新会话仓储抽象。
 * 职责：为 refresh token 轮换与撤销提供最小持久化语义。
 * 边界：这里只定义业务语义，不暴露数据库实现细节。
 */
public interface AuthRefreshSessionRepository {

    /**
     * 按会话 ID 查询刷新会话。
     *
     * @param sessionId 刷新会话 ID
     * @return 命中时返回会话，未命中时返回空
     */
    Optional<AuthRefreshSession> findById(long sessionId);

    /**
     * 保存刷新会话。
     *
     * @param session 刷新会话
     * @return 已保存会话
     */
    AuthRefreshSession save(AuthRefreshSession session);

    /**
     * 撤销刷新会话。
     *
     * @param sessionId 刷新会话 ID
     */
    void revoke(long sessionId);
}
