package team.carrypigeon.backend.chat.domain.features.auth.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;

/**
 * 鉴权账户仓储抽象。
 * 职责：为应用层提供账户唯一性判断与持久化语义入口。
 * 边界：这里只定义业务语义，不暴露数据库服务实现细节。
 */
public interface AuthAccountRepository {

    /**
     * 按用户名查询账户。
     *
     * @param username 当前服务端内用户名
     * @return 命中时返回账户，未命中时返回空
     */
    Optional<AuthAccount> findByUsername(String username);

    /**
     * 保存新账户。
     *
     * @param account 待保存账户
     * @return 已保存账户
     */
    AuthAccount save(AuthAccount account);
}
