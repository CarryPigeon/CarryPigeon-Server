package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

/**
 * 用户资料数据库服务抽象。
 * 职责：提供用户资料表级通用元能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface UserProfileDatabaseService {

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @return 命中时返回资料记录，未命中时返回空
     */
    Optional<UserProfileRecord> findById(long accountId);

    /** 事务内以行锁读取资料，不使用展示缓存。 */
    Optional<UserProfileRecord> findByIdForUpdate(long accountId);

    /**
     * 按账户 ID 批量读取用户资料。
     *
     * @param accountIds 账户 ID 列表
     * @return 命中的资料记录列表
     */
    List<UserProfileRecord> findByIds(List<Long> accountIds);

    /**
     * 分页查询用户资料记录。
     * 说明：按账户 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页资料记录
     */
    PageResult<UserProfileRecord> page(PageQuery page);

    /**
     * 写入新的用户资料记录。
     *
     * @param record 待持久化资料记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(UserProfileRecord record);

    /**
     * 批量写入用户资料记录。
     *
     * @param records 待持久化资料记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<UserProfileRecord> records);

    /**
     * 更新用户资料记录。
     *
     * @param record 待更新资料记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(UserProfileRecord record);

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
