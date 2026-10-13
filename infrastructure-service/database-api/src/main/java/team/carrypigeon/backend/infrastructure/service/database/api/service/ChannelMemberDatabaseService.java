package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

/**
 * 频道成员数据库服务抽象。
 * 职责：向 chat-domain 提供频道成员最小读写能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelMemberDatabaseService {

    /**
     * 保存频道成员关系。
     *
     * @param record 成员关系
     */
    void insert(ChannelMemberRecord record);

    /**
     * 查询活跃成员投影。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回活跃成员
     */
    Optional<ChannelMemberRecord> findByRelation(long channelId, long accountId);

    /**
     * 更新已存在的活跃成员投影。
     *
     * @param record 待更新成员
     */
    void update(ChannelMemberRecord record);

    /**
     * 删除已存在的活跃成员投影。
     *
     * @param memberId 成员 ID
     */
    void delete(long memberId);

    /**
     * 删除频道下的全部成员关系。
     * 默认实现保持内存替身兼容，持久化适配器应覆盖为集合级删除。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /**
     * 查询频道下的全部活跃成员投影。
     *
     * @param channelId 频道 ID
     * @return 活跃成员列表
     */
    List<ChannelMemberRecord> findMembersByChannelId(long channelId);

    /**
     * 查询账户参与的全部频道成员。
     *
     * @param accountId 账户 ID
     * @return 频道列表
     */
    List<ChannelMemberRecord> findMembersByAccountId(long accountId);
}
