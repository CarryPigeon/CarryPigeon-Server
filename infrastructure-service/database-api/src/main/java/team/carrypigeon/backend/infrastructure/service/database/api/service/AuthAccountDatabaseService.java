package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;

/**
 * 鉴权账户数据库服务抽象。
 * 职责：提供鉴权账户表级通用元能力与账户查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface AuthAccountDatabaseService {

    /**
     * 按账户 ID 查询鉴权账户。
     *
     * @param accountId 账户 ID
     * @return 命中时返回账户记录，未命中时返回空
     */
    Optional<AuthAccountRecord> findById(long accountId);

    /**
     * 按账户 ID 批量读取鉴权账户。
     *
     * @param accountIds 账户 ID 列表
     * @return 命中的账户记录列表
     */
    List<AuthAccountRecord> findByIds(List<Long> accountIds);

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
     * 分页查询鉴权账户记录。
     * 说明：按账户 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页账户记录
     */
    DatabasePageResult<AuthAccountRecord> page(DatabasePageQuery page);

    /**
     * 写入新的鉴权账户记录。
     *
     * @param record 待持久化账户记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(AuthAccountRecord record);

    /**
     * 批量写入鉴权账户记录。
     *
     * @param records 待持久化账户记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<AuthAccountRecord> records);

    /**
     * 更新鉴权账户记录。
     *
     * @param record 待更新账户记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(AuthAccountRecord record);

    /**
     * 按账户 ID 删除记录。
     *
     * @param accountId 账户 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long accountId);

    /**
     * 按账户 ID 批量删除记录。
     *
     * @param accountIds 账户 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> accountIds);
}
