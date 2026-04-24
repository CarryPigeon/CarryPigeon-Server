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
     * 判断指定账户是否为频道成员。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 属于成员时返回 true
     */
    boolean exists(long channelId, long accountId);

    /**
     * 写入新的频道成员记录。
     *
     * @param record 待持久化成员记录
     */
    void insert(ChannelMemberRecord record);

    /**
     * 查询活跃成员记录。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员记录
     */
    Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId);

    /**
     * 更新已存在的成员记录。
     *
     * @param record 待更新成员记录
     */
    void update(ChannelMemberRecord record);

    /**
     * 删除已存在的成员记录。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     */
    void delete(long channelId, long accountId);

    /**
     * 查询频道下的全部成员记录。
     *
     * @param channelId 频道 ID
     * @return 成员记录列表
     */
    default List<ChannelMemberRecord> findByChannelId(long channelId) {
        return List.of();
    }

    /**
     * 查询频道下的全部成员账户 ID。
     *
     * @param channelId 频道 ID
     * @return 成员账户 ID 列表
     */
    List<Long> findAccountIdsByChannelId(long channelId);
}
