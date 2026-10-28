package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;

/**
 * 频道申请数据库服务抽象。
 * 职责：提供频道申请表级通用元能力与申请查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelApplicationDatabaseService {

    /**
     * 写入新的申请记录。
     *
     * @param record 待持久化申请记录
     */
    void insert(ChannelApplicationRecord record);

    /**
     * 批量写入申请记录。
     *
     * @param records 待持久化申请记录
     */
    void insertAll(List<ChannelApplicationRecord> records);

    /**
     * 按申请 ID 删除记录。
     *
     * @param applicationId 申请 ID
     */
    void delete(long applicationId);

    /**
     * 按申请 ID 批量删除记录。
     *
     * @param applicationIds 申请 ID 列表
     */
    void deleteAll(List<Long> applicationIds);

    /**
     * 更新既有申请记录。
     *
     * @param record 待更新申请记录
     */
    void update(ChannelApplicationRecord record);

    /**
     * 查询申请表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 申请记录列表
     */
    List<ChannelApplicationRecord> list();

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
    Optional<ChannelApplicationRecord> findByChannelAndApplicant(long channelId, long applicantId);

    /**
     * 查询频道下的全部申请记录。
     *
     * @param channelId 频道 ID
     * @return 申请记录列表
     */
    List<ChannelApplicationRecord> listByChannelId(long channelId);
}
