package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及数据库服务抽象。
 * 职责：提供提及表级元能力与通用查询。
 * 边界：不承载已读状态流转；read 过滤值由 Repository 传入。
 */
public interface MentionDatabaseService {

    /**
     * 按提及 ID 查询记录。
     *
     * @param mentionId 提及 ID
     * @return 命中时返回记录，未命中时返回空
     */
    Optional<MentionRecord> findById(long mentionId);

    /**
     * 按提及 ID 批量查询记录。
     *
     * @param mentionIds 提及 ID 列表
     * @return 命中的提及记录列表
     */
    List<MentionRecord> findByIds(List<Long> mentionIds);

    /** 读取当前收件人在仍加入频道内的提及，空集合返回空列表。 */
    List<MentionRecord> findVisibleByIds(long accountId, List<Long> mentionIds);

    /** 标记一条可见提及，重复标记成功，不可见时返回 false。 */
    boolean markAsRead(long accountId, long mentionId);

    /**
     * 按通用条件查询提及记录。
     * 输出：按提及 ID 倒序返回，最多 query.limit 条。
     *
     * @param query 提及查询条件
     * @return 提及记录列表
     */
    List<MentionRecord> find(MentionQuery query);

    /**
     * 分页查询提及记录。
     * 说明：按提及 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页提及记录
     */
    PageResult<MentionRecord> page(PageQuery page);

    /**
     * 写入单条提及记录。
     *
     * @param record 待写入记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(MentionRecord record);

    /**
     * 批量写入提及记录。
     * 约束：空集合不产生数据库访问。
     *
     * @param records 待写入提及记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<MentionRecord> records);

    /**
     * 将指定账号当前匹配的未读提及集合更新为已读，不读取完整记录或插入数据。
     * throughMentionId 是包含端点的上界，channelId 是可选频道；为空时不施加对应条件。
     * 无上界时以这条 UPDATE 实际匹配的集合为准，返回从未读变为已读的行数。
     */
    int markAllAsRead(long accountId, Long throughMentionId, Long channelId);

    /**
     * 更新完整提及记录。
     *
     * @param record 待更新记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(MentionRecord record);

    /**
     * 批量写入完整提及记录；不存在时插入，存在时覆盖。
     *
     * @param records 待更新记录
     * @return SQL 成功执行时为 true；空集合返回 true，失败时抛出异常
     */
    boolean updateAll(List<MentionRecord> records);

    /**
     * 按提及 ID 删除记录。
     *
     * @param mentionId 提及 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long mentionId);

    /**
     * 按提及 ID 批量删除记录。
     *
     * @param mentionIds 提及 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> mentionIds);

    /**
     * 删除指定消息产生的全部提及记录。
     *
     * @param messageId 消息 ID
     */
    void deleteByMessageId(long messageId);

    /** 删除频道下的全部提及记录。 */
    void deleteByChannelId(long channelId);
}
