package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;

/**
 * 频道申请数据库服务抽象。
 * 职责：提供频道申请表级通用元能力与申请查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelApplicationDatabaseService {

    /**
     * 按申请 ID 查询申请记录。
     *
     * @param applicationId 申请 ID
     * @return 命中时返回申请记录，未命中时返回空
     */
    Optional<ChannelApplicationRecord> findById(long applicationId);

    /**
     * 按申请 ID 批量查询申请记录。
     *
     * @param applicationIds 申请 ID 列表
     * @return 命中的申请记录列表
     */
    List<ChannelApplicationRecord> findByIds(List<Long> applicationIds);

    /**
     * 查询频道下目标账户的申请记录。
     *
     * @param channelId 频道 ID
     * @param applicantId 申请人账户 ID
     * @return 命中时返回申请记录，未命中时返回空
     */
    Optional<ChannelApplicationRecord> findByChannelIdAndApplicantId(long channelId, long applicantId);

    /**
     * 分页查询申请记录。
     * 说明：按申请 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页申请记录
     */
    PageResult<ChannelApplicationRecord> page(PageQuery page);

    /**
     * 分页查询频道下的申请记录。
     * 输出：按申请 ID 倒序返回，供申请列表分页使用。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页申请记录
     */
    PageResult<ChannelApplicationRecord> pageByChannelId(long channelId, PageQuery page);

    /**
     * 写入新的申请记录。
     *
     * @param record 待持久化申请记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(ChannelApplicationRecord record);

    /**
     * 批量写入申请记录。
     *
     * @param records 待持久化申请记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<ChannelApplicationRecord> records);

    /**
     * 按申请 ID、频道 ID 和 PENDING 状态条件更新，仅修改状态及响应时间。
     *
     * @param record 审批结果，status 为 ACCEPTED 或 DECLINED，respondedAt 非空
     * @return 恰好更新一条待处理申请时为 true；条件未命中时为 false
     */
    boolean decideIfPending(ChannelApplicationRecord record);

    /** 删除该频道最多 limit 条关系，返回已删除记录用于进度及通知；只用于已撤下频道的清理事务。 */
    List<ChannelApplicationRecord> deleteBatchByChannelId(long channelId, int limit);

    /**
     * 按申请 ID 删除记录。
     *
     * @param applicationId 申请 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long applicationId);

    /**
     * 按申请 ID 批量删除记录。
     *
     * @param applicationIds 申请 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> applicationIds);

    /**
     * 删除频道下的全部申请记录。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);
}
