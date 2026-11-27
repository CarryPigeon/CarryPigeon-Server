package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.CPChannelMessage;

/**
 * 消息仓储抽象。
 * 职责：定义频道消息的业务语义读写入口。
 * 边界：不暴露数据库实现细节，不通过可选参数切换查询方向。
 */
public interface MessageRepository {

    /** 清理已撤下频道的消息侧数据，必须在事务中调用。 */
    void deleteByChannelId(long channelId);

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
     * @return 消息列表，按消息 ID 倒序排列
     */
    List<CPChannelMessage> findLatestByChannelId(long channelId, int limit);

    /**
     * 查询频道内指定消息之前的更旧消息。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 排他游标消息 ID
     * @param limit 查询条数
     * @return 消息列表，按消息 ID 倒序排列
     */
    List<CPChannelMessage> findBeforeByChannelId(long channelId, long cursorMessageId, int limit);

    /**
     * 查询频道内指定消息之后的更新消息。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 排他游标消息 ID
     * @param limit 查询条数
     * @return 消息列表，按消息 ID 升序排列
     */
    List<CPChannelMessage> findAfterByChannelId(long channelId, long cursorMessageId, int limit);

    /**
     * 在频道内按关键字和过滤条件搜索消息。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param cursorMessageId 搜索排他游标消息 ID，可为空
     * @param senderAccountId 发送者账户 ID，可为空
     * @param domain 消息 domain，可为空
     * @param limit 查询条数
     * @return 搜索命中消息列表，按消息 ID 倒序排列
     */
    List<CPChannelMessage> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            int limit
    );

    /**
     * 按消息 ID 列查询消息。
     *
     * @param messageIds 消息 ID 列
     * @return 查询命中消息集合
     */
    Collection<CPChannelMessage> findByIds(List<Long> messageIds);

    /** 入群事务已持有频道锁。读取该频道当前最大的消息 ID，空频道返回 null。 */
    Long findLatestMessageIdForJoin(long channelId);
}
