package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

/**
 * 频道已读状态数据库服务抽象。
 * 职责：向 chat-domain 提供频道已读锚点读写与未读统计查询能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelReadStateDatabaseService {

    /**
     * 查询账户在频道中的已读状态。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回已读状态记录，未命中时返回空
     */
    Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId);

    /**
     * 新增或覆盖频道已读状态。
     *
     * @param record 待写入已读状态记录
     */
    void upsert(ChannelReadStateRecord record);

    /**
     * 仅在锚点更靠后时推进已读状态。
     *
     * @param record 待推进已读状态记录
     * @return 实际推进时返回 true；锚点不比已存值更靠后时返回 false
     */
    boolean advanceIfNewer(ChannelReadStateRecord record);

    /**
     * 查询账户各频道的未读统计投影。
     * 输出：按频道 ID 升序返回，只包含存在未读消息的频道。
     *
     * @param accountId 账户 ID
     * @return 未读统计投影列表
     */
    List<ChannelUnreadRecord> findUnreadsByAccountId(long accountId);
}
