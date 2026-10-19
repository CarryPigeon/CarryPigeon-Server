package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

/**
 * 用户资料数据库服务抽象。
 * 职责：向 chat-domain 提供用户资料最小查询与写入能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface UserProfileDatabaseService {

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @return 命中时返回资料记录，未命中时返回空
     */
    Optional<UserProfileRecord> findByAccountId(long accountId);

    /**
     * 批量读取用户资料。
     * 输出：未命中的账户 ID 不进入结果，顺序不保证。
     *
     * @param accountIds 账户 ID 集合
     * @return 命中的资料记录列表
     */
    List<UserProfileRecord> findByIds(Collection<Long> accountIds);

    /**
     * 写入新的用户资料记录。
     *
     * @param record 待持久化资料记录
     */
    void insert(UserProfileRecord record);

    /**
     * 更新用户资料记录。
     *
     * @param record 待更新资料记录
     */
    void update(UserProfileRecord record);
}
