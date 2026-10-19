package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;

/**
 * 鉴权账户数据库服务抽象。
 * 职责：向 chat-domain 提供鉴权账户最小查询与写入能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface AuthAccountDatabaseService {

    /**
     * 按用户名查询鉴权账户。
     *
     * @param username 当前服务端内的用户名
     * @return 命中时返回账户记录，未命中时返回空
     */
    Optional<AuthAccountRecord> findByUsername(String username);

    /**
     * 按邮箱查询鉴权账户。
     *
     * @param email 账户邮箱
     * @return 命中时返回账户记录，未命中时返回空
     */
    Optional<AuthAccountRecord> findByEmail(String email);

    /**
     * 按账户 ID 查询鉴权账户。
     *
     * @param accountId 账户 ID
     * @return 命中时返回账户记录，未命中时返回空
     */
    Optional<AuthAccountRecord> findById(long accountId);

    /**
     * 批量读取鉴权账户，供账户列表投影避免逐账户查询。
     *
     * @param accountIds 账户 ID 集合
     * @return 命中的账户记录列表
     */
    List<AuthAccountRecord> findByIds(Collection<Long> accountIds);

    /**
     * 写入新的鉴权账户记录。
     *
     * @param record 待持久化账户记录
     */
    void insert(AuthAccountRecord record);

    /**
     * 更新鉴权账户记录。
     *
     * @param record 待更新账户记录
     */
    void update(AuthAccountRecord record);
}
