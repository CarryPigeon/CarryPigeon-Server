package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;

/**
 * 鉴权账户数据库服务抽象。
 * 职责：向 chat-domain 提供最小账户查询与写入能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface AuthAccountDatabaseService {

    /**
     * 按用户名查询账户记录。
     *
     * @param username 当前服务端内的用户名
     * @return 命中时返回账户记录，未命中时返回空
     */
    Optional<AuthAccountRecord> findByUsername(String username);

    /**
     * 写入新的鉴权账户记录。
     *
     * @param record 待持久化的账户记录
     */
    void insert(AuthAccountRecord record);
}
