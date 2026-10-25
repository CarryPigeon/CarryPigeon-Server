package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及数据库服务抽象。
 * 职责：提供提及表级元能力与通用查询。
 * 边界：不承载已读状态流转；read 过滤值由 Repository 传入。
 */
public interface MentionDatabaseService {

    /**
     * 写入单条提及记录。
     *
     * @param record 待写入记录
     */
    void insert(MentionRecord record);

    /**
     * 批量写入提及记录。
     * 约束：空集合不产生数据库访问。
     *
     * @param records 待写入提及记录
     */
    void insertAll(List<MentionRecord> records);

    /**
     * 按提及 ID 删除记录。
     *
     * @param mentionId 提及 ID
     */
    void delete(long mentionId);

    /**
     * 按提及 ID 批量删除记录。
     *
     * @param mentionIds 提及 ID 列表
     */
    void deleteAll(List<Long> mentionIds);

    /**
     * 更新完整提及记录。
     *
     * @param record 待更新记录
     */
    void update(MentionRecord record);

    /**
     * 查询提及表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 提及记录列表
     */
    List<MentionRecord> list();

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

    /**
     * 批量更新完整提及记录。
     *
     * @param records 待更新记录
     */
    void updateAll(List<MentionRecord> records);

    /**
     * 删除指定消息产生的全部提及记录。
     *
     * @param messageId 消息 ID
     */
    void deleteByMessageId(long messageId);

    /**
     * 按通用条件查询提及记录。
     * 输出：按提及 ID 倒序返回，最多 query.limit 条。
     *
     * @param query 提及查询条件
     * @return 提及记录列表
     */
    List<MentionRecord> find(MentionQuery query);
}
