package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

/**
 * 频道成员数据库服务抽象。
 * 职责：向 chat-domain 提供频道成员关系的最小读写能力。
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
     * 按频道与账户定位成员关系。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员关系，未命中时返回空
     */
    Optional<ChannelMemberRecord> findByRelation(long channelId, long accountId);

    /**
     * 更新既有成员关系。
     *
     * @param record 待更新成员关系
     */
    void update(ChannelMemberRecord record);

    /**
     * 按成员关系 ID 删除成员关系。
     *
     * @param memberId 成员关系 ID
     */
    void delete(long memberId);

    /**
     * 删除频道下的全部成员关系。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /**
     * 查询频道下的全部成员关系。
     * 输出：顺序不保证。
     *
     * @param channelId 频道 ID
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> findMembersByChannelId(long channelId);

    /**
     * 查询账户参与的全部成员关系。
     * 输出：顺序不保证。
     *
     * @param accountId 账户 ID
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> findMembersByAccountId(long accountId);
}
