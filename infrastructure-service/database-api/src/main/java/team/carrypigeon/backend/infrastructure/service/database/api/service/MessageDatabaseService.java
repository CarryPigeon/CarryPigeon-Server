package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

/**
 * 消息数据库服务抽象。
 * 职责：提供消息表级通用元能力、通用查询与搜索例外。
 * 边界：不承载消息业务规则；搜索条件由调用方传入，不在本层解释 status、时间线或权限语义。
 */
public interface MessageDatabaseService {

    /**
     * 写入新的消息记录。
     *
     * @param record 待持久化消息记录
     */
    void insert(MessageRecord record);

    /**
     * 批量写入消息记录。
     *
     * @param records 待持久化消息记录
     */
    void insertAll(List<MessageRecord> records);

    /**
     * 按消息 ID 删除消息记录。
     *
     * @param messageId 消息 ID
     */
    void delete(long messageId);

    /**
     * 按消息 ID 批量删除消息记录。
     *
     * @param messageIds 消息 ID 列表
     */
    void deleteAll(List<Long> messageIds);

    /**
     * 更新已有消息记录。
     * 边界：更新完整行；哪些字段允许变化由 Repository 决定。
     *
     * @param record 待更新消息记录
     */
    void update(MessageRecord record);

    /**
     * 查询消息表全部记录。
     * 说明：仅供元能力完整性与离线维护使用，业务查询应使用 find 或专用搜索。
     *
     * @return 消息记录列表
     */
    List<MessageRecord> list();

    /**
     * 按消息 ID 查询消息记录。
     *
     * @param messageId 消息 ID
     * @return 命中时返回消息记录，未命中时返回空
     */
    Optional<MessageRecord> findById(long messageId);

    /**
     * 按消息 ID 批量查询消息记录。
     *
     * @param messageIds 消息 ID 列表
     * @return 命中的消息记录列表
     */
    List<MessageRecord> findByIds(List<Long> messageIds);

    /**
     * 按通用消息查询条件读取消息。
     * 输出：按消息 ID 排序，最多 query.limit 条。
     * 边界：只应用数据谓词，不判断时间线或状态业务含义。
     *
     * @param query 消息查询条件
     * @return 命中的消息记录列表
     */
    List<MessageRecord> find(MessageQuery query);

    /**
     * 在频道内按关键字搜索消息。
     * 说明：搜索是专用查询例外；status、keyword 等条件均由 Repository 传入。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param cursorMessageId 游标消息 ID，可为空
     * @param senderAccountId 发送者账户 ID，可为空
     * @param domain 消息 domain，可为空
     * @param beforeMessageId 前界消息 ID，可为空
     * @param afterMessageId 后界消息 ID，可为空
     * @param status 消息状态，可为空
     * @param limit 返回条数
     * @return 搜索命中消息记录列表
     */
    List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            Long beforeMessageId,
            Long afterMessageId,
            String status,
            int limit
    );
}
