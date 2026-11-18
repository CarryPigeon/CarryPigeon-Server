package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageSendRequest;

import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;

/**
 * 消息仓储抽象。
 * 职责：定义频道消息的业务语义读写入口。
 * 边界：不暴露数据库实现细节，不通过可选参数切换查询方向。
 */
public interface MessageRepository {

    /** 清理已撤下频道的消息侧数据，必须在事务中调用。 */
    void deleteByChannelId(long channelId);


    /**
     * 在发送事务中预占请求 ID；同一发送者和频道的并发重试等待首个事务结束。
     * 新记录使用 proposedMessageId；已有记录返回原摘要与消息 ID，回滚不能留下占位。
     * 幂等记录不按时间自动过期，不能因原消息缺失而重建发送请求。
     */
    MessageSendRequest reserveSend(long accountId, long channelId, UUID requestId, String payloadHash, long proposedMessageId);

    /**
     * 分配频道内下一消息序号，调用方必须在同一事务中保存消息。
     * 序号行锁一直持有到事务结束，保证后续序号不会先于当前消息提交可见；回滚同时撤销分配。
     */
    long nextSequence(long channelId);

    /**
     * 保存频道消息。
     *
     * @param message 待持久化消息
     */
    void save(CPChannelMessage message);

    /**
     * 按消息 ID 查询消息。
     *
     * @param messageId 消息 ID
     * @return 命中时返回消息
     */
    Optional<CPChannelMessage> findById(long messageId);

    /** 在事务内以当前读查询重试结果，避免先前授权查询建立的快照遗漏刚提交的原消息。 */
    Optional<CPChannelMessage> findByIdForUpdate(long messageId);

    /**
     * 更新已存在的频道消息。
     *
     * @param message 待更新消息
     */
    void update(CPChannelMessage message);

    /**
     * 查询频道最新消息。
     *
     * @param channelId 频道 ID
     * @param limit 查询条数
     * @return 消息列表，按频道序号倒序排列
     */
    List<CPChannelMessage> findLatestByChannelId(long channelId, int limit);

    /**
     * 查询频道内指定消息之前的更旧消息。
     *
     * @param channelId 频道 ID
     * @param cursorSequence 排他游标频道序号
     * @param limit 查询条数
     * @return 消息列表，按频道序号倒序排列
     */
    List<CPChannelMessage> findBeforeByChannelId(long channelId, long cursorSequence, int limit);

    /**
     * 查询频道内指定消息之后的更新消息。
     *
     * @param channelId 频道 ID
     * @param cursorSequence 排他游标频道序号
     * @param limit 查询条数
     * @return 消息列表，按频道序号升序排列
     */
    List<CPChannelMessage> findAfterByChannelId(long channelId, long cursorSequence, int limit);

    /**
     * 在频道内按关键字和过滤条件搜索消息。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param cursorSequence 搜索排他游标频道序号，可为空
     * @param senderAccountId 发送者账户 ID，可为空
     * @param domain 消息 domain，可为空
     * @param limit 查询条数
     * @return 搜索命中消息列表，按频道序号倒序排列
     */
    List<CPChannelMessage> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorSequence,
            Long senderAccountId,
            String domain,
            int limit
    );
}
