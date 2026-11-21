package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelUnread;

/**
 * 频道已读状态仓储抽象。
 */
public interface ChannelReadStateRepository {

    /** 删除频道下的全部已读状态。 */
    void deleteByChannelId(long channelId);

    Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId);

    /** 在持有频道锁的写事务内锁定并读取当前锚点。 */
    Optional<ChannelReadState> findForUpdate(long channelId, long accountId);

    /** 保存已经校验为前进的锚点；调用方须在同一写事务内持有频道锁。 */
    void save(ChannelReadState readState);

    List<ChannelUnread> listUnreadsByAccountId(long accountId);

    /** 以给定消息 ID 为排他下界，统计成员在单个频道最多 10000 条有效未读。 */
    long countUnreads(long channelId, long accountId, long afterMessageId);
}
