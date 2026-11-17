package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageEventRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageEventPageRecord;

/** 消息变更流与 outbox 存储契约；所有操作均加入调用方数据库事务，不独立提交业务变更。 */
public interface MessageEventDatabaseService {
    /** 事务内持锁分配频道事件序号并追加事件；回滚同时撤销序号，ID 不承担顺序语义。 */
    void append(long eventId, long channelId, long messageId, String type, String payload);

    /** 撤回事务中清除创建事件的原始正文，避免恢复流绕过撤回后的消息可见性。 */
    void redactCreated(long channelId, long messageId, String replacementPayload);

    /** 锁定流边界并读取排他游标后的事件；afterSequence 为空时只返回当前边界，用于先建立快照恢复锚点。 */
    MessageEventPageRecord readChanges(long channelId, Long afterSequence, int limit);

    /** 按待派发状态领取并持锁，跳过其它工作进程已锁定的事件；不领取保留窗口以前的事件。 */
    Optional<MessageEventRecord> claimPending(Instant cutoff);

    /** 仅表示已经尝试向本机在线连接派发，不代表客户端确认；记录仍留在恢复流内。 */
    void markDelivered(long eventId);

    /** 保留事件并增加尝试次数，按有上限的退避时间再次尝试。 */
    void retryLater(long eventId);

    /** 限量清理一个频道已过期的连续前缀，同时推进过期边界；返回实际删除条数。 */
    int purgeExpired(Instant cutoff, int limit);

    /** 分批删除已撤下频道的变更记录，清空后删除流边界；返回是否已清理完成。 */
    boolean deleteChannelBatch(long channelId, int limit);
}
