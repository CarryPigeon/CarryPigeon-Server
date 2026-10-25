package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

/**
 * 频道已读状态数据库服务抽象。
 * 职责：提供已读状态表级通用元能力、锁定读与专用未读读模型查询。
 * 边界：不承载“只前进不后退”等业务规则；该规则由 Repository 在锁定读后判断。
 */
public interface ChannelReadStateDatabaseService {

    /**
     * 写入新的已读状态记录。
     *
     * @param record 待写入记录
     */
    void insert(ChannelReadStateRecord record);

    /**
     * 批量写入已读状态记录。
     *
     * @param records 待写入记录
     */
    void insertAll(List<ChannelReadStateRecord> records);

    /**
     * 按复合主键删除记录。
     *
     * @param key 复合主键
     */
    void delete(ChannelReadStateKey key);

    /**
     * 按复合主键批量删除记录。
     *
     * @param keys 复合主键列表
     */
    void deleteAll(List<ChannelReadStateKey> keys);

    /**
     * 按复合主键更新完整已读状态记录。
     *
     * @param record 待更新记录
     */
    void update(ChannelReadStateRecord record);

    /**
     * 查询已读状态表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 已读状态记录列表
     */
    List<ChannelReadStateRecord> list();

    /**
     * 按复合主键查询记录。
     *
     * @param key 复合主键
     * @return 命中时返回记录，未命中时返回空
     */
    Optional<ChannelReadStateRecord> findById(ChannelReadStateKey key);

    /**
     * 按复合主键批量查询记录。
     *
     * @param keys 复合主键列表
     * @return 命中的记录列表
     */
    List<ChannelReadStateRecord> findByIds(List<ChannelReadStateKey> keys);

    /**
     * 按复合主键锁定读取记录。
     * 约束：必须在事务内调用。
     *
     * @param key 复合主键
     * @return 命中时返回记录，未命中时返回空
     */
    Optional<ChannelReadStateRecord> findByIdForUpdate(ChannelReadStateKey key);

    /**
     * 新增或覆盖频道已读状态。
     *
     * @param record 待写入已读状态记录
     */
    void upsert(ChannelReadStateRecord record);

    /**
     * 查询账户各频道的未读统计投影。
     * 说明：跨表聚合读模型是性能例外，不解释权限或时间线规则。
     *
     * @param accountId 账户 ID
     * @return 未读统计投影列表
     */
    List<ChannelUnreadRecord> findUnreadsByAccountId(long accountId);
}
