package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

/**
 * 频道成员数据库服务抽象。
 * 职责：提供频道成员表级通用元能力与关系查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelMemberDatabaseService {

    /**
     * 插入新的成员关系。
     *
     * @param record 待持久化成员关系
     */
    void insert(ChannelMemberRecord record);

    /**
     * 批量插入成员关系。
     *
     * @param records 待持久化成员关系
     */
    void insertAll(List<ChannelMemberRecord> records);

    /**
     * 按成员关系 ID 删除成员关系。
     *
     * @param memberId 成员关系 ID
     */
    void delete(long memberId);

    /**
     * 按成员关系 ID 批量删除成员关系。
     *
     * @param memberIds 成员关系 ID 列表
     */
    void deleteAll(List<Long> memberIds);

    /**
     * 更新既有成员关系。
     *
     * @param record 待更新成员关系
     */
    void update(ChannelMemberRecord record);

    /**
     * 查询频道成员表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> list();

    /**
     * 按成员关系 ID 查询。
     *
     * @param memberId 成员关系 ID
     * @return 命中时返回成员关系，未命中时返回空
     */
    Optional<ChannelMemberRecord> findById(long memberId);

    /**
     * 按成员关系 ID 批量查询。
     *
     * @param memberIds 成员关系 ID 列表
     * @return 命中的成员关系列表
     */
    List<ChannelMemberRecord> findByIds(List<Long> memberIds);

    /**
     * 按频道与账户定位成员关系。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员关系，未命中时返回空
     */
    Optional<ChannelMemberRecord> findByRelation(long channelId, long accountId);

    /**
     * 删除频道下的全部成员关系。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /**
     * 查询频道下的全部成员关系。
     *
     * @param channelId 频道 ID
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> findMembersByChannelId(long channelId);

    /**
     * 查询账户参与的全部成员关系。
     *
     * @param accountId 账户 ID
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> findMembersByAccountId(long accountId);
}
