package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

/**
 * 频道成员数据库服务抽象。
 * 职责：提供频道成员表级通用元能力与关系查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelMemberDatabaseService {

    /** 直接查询仍存在频道的成员关系，仅返回候选账号中的有效成员；空集合返回空。 */
    List<Long> filterMemberAccountIds(long channelId, List<Long> accountIds);

    /** 直接查询账号已加入且仍存在的频道 ID，用于认证连接建立在线订阅。 */
    List<Long> findChannelIdsByAccountId(long accountId);

    /**
     * 按频道与账户定位成员关系。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员关系，未命中时返回空
     */
    Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId);

    /** 授权专用直读；不读取或回填缓存，缺失时返回空。可见性遵守调用方事务隔离级别。 */
    Optional<ChannelMemberRecord> findByChannelIdAndAccountIdDirect(long channelId, long accountId);

    /**
     * 锁定并读取最新成员关系，不使用缓存或事务旧快照；不存在时返回空。
     * 必须在事务内先锁定所属频道，遵守频道在前、成员在后的治理锁顺序。
     */
    Optional<ChannelMemberRecord> findByChannelIdAndAccountIdForUpdate(long channelId, long accountId);



    /**
     * 按成员关系 ID 查询。
     *
     * @param memberId 成员关系 ID
     * @return 命中时返回成员关系，未命中时返回空
     */
    Optional<ChannelMemberRecord> findById(long memberId);

    /**
     * 按成员关系 ID 批量查询。
     *
     * @param memberIds 成员关系 ID 列表
     * @return 命中的成员关系列表
     */
    List<ChannelMemberRecord> findByIds(List<Long> memberIds);



    /**
     * 查询频道下的全部成员关系。
     * 说明：这是显式全量快照能力，供通知接收人与治理快照使用，不参与分页列表。
     *
     * @param channelId 频道 ID
     * @return 成员关系列表
     */
    List<ChannelMemberRecord> findMembersByChannelId(long channelId);

    /**
     * 分页查询频道成员记录。
     * 说明：按成员关系 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页成员关系记录
     */
    PageResult<ChannelMemberRecord> page(PageQuery page);

    /**
     * 分页查询账户参与的全部成员关系。
     * 输出：按频道 ID 倒序返回，供频道列表分页使用。
     *
     * @param accountId 账户 ID
     * @param page 分页参数
     * @return 分页成员关系记录
     */
    PageResult<ChannelMemberRecord> pageByAccountId(long accountId, PageQuery page);

    /**
     * 分页查询频道下的成员关系。
     * 输出：按成员关系 ID 倒序返回，供频道成员列表分页使用。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页成员关系记录
     */
    PageResult<ChannelMemberRecord> pageByChannelId(long channelId, PageQuery page);

    /**
     * 插入新的成员关系。
     *
     * @param record 待持久化成员关系
     * @return 恰好插入一条时为 true
     */
    boolean insert(ChannelMemberRecord record);

    /**
     * 批量插入成员关系。
     *
     * @param records 待持久化成员关系
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<ChannelMemberRecord> records);

    /**
     * 更新既有成员关系。
     *
     * @param record 待更新成员关系
     * @return 恰好更新一条记录时为 true；未命中时为 false
     */
    boolean update(ChannelMemberRecord record);

    /**
     * 按成员关系 ID 删除成员关系。
     *
     * @param memberId 成员关系 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long memberId);

    /**
     * 按成员关系 ID 批量删除成员关系。
     *
     * @param memberIds 成员关系 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> memberIds);

    /**
     * 删除频道下的全部成员关系。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);
}
