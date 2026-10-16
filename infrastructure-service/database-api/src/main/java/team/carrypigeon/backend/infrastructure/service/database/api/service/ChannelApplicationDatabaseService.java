package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;

/**
 * 频道邀请数据库服务抽象。
 * 职责：向 chat-domain 提供频道邀请记录的最小读写能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelApplicationDatabaseService {

    /**
     * 查询频道下目标账户的申请记录。
     *
     * @param channelId 频道 ID
     * @param applicantId 被申请账户 ID
     * @return 命中时返回申请记录
     */
    Optional<ChannelApplicationRecord> findByChannelAndApplicant(long channelId, long applicantId);

    /**
     * 按申请 ID 查询。
     */
    Optional<ChannelApplicationRecord> findById(long applicationId);

    /**
     * 查询频道下的全部申请记录。
     */
    List<ChannelApplicationRecord> findByChannelId(long channelId);

    /**
     * 写入新的申请记录。
     *
     * @param record 待持久化申请记录
     */
    void insert(ChannelApplicationRecord record);

    /**
     * 更新已存在的申请记录。
     *
     * @param record 待更新申请记录
     */
    void update(ChannelApplicationRecord record);

    /**
     * 删除已存在的申请记录
     *
     * @param applicationId 申请id
     * */
    void delete(long applicationId);

}
