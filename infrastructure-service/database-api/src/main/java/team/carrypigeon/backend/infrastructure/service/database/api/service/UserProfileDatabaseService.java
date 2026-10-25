package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

/**
 * 用户资料数据库服务抽象。
 * 职责：提供用户资料表级通用元能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface UserProfileDatabaseService {

    /**
     * 写入新的用户资料记录。
     *
     * @param record 待持久化资料记录
     */
    void insert(UserProfileRecord record);

    /**
     * 批量写入用户资料记录。
     *
     * @param records 待持久化资料记录
     */
    void insertAll(List<UserProfileRecord> records);

    /**
     * 按账户 ID 删除记录。
     *
     * @param accountId 账户 ID
     */
    void delete(long accountId);

    /**
     * 按账户 ID 批量删除记录。
     *
     * @param accountIds 账户 ID 列表
     */
    void deleteAll(List<Long> accountIds);

    /**
     * 更新用户资料记录。
     *
     * @param record 待更新资料记录
     */
    void update(UserProfileRecord record);

    /**
     * 查询用户资料表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 资料记录列表
     */
    List<UserProfileRecord> list();

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @return 命中时返回资料记录，未命中时返回空
     */
    Optional<UserProfileRecord> findById(long accountId);

    /**
     * 按账户 ID 批量读取用户资料。
     *
     * @param accountIds 账户 ID 列表
     * @return 命中的资料记录列表
     */
    List<UserProfileRecord> findByIds(List<Long> accountIds);
}
