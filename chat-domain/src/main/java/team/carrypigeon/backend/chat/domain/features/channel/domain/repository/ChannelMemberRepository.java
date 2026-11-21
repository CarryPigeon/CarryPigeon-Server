package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

/**
 * 频道成员仓储抽象。
 * 职责：以业务语义表达账号与频道之间活跃成员关系的读写入口。
 * 边界：不暴露数据库实现细节、不承载角色规则与治理权限判断；成员关系一旦删除即不可在此恢复。
 */
public interface ChannelMemberRepository {

    /** 直接查询账号已加入且仍存在的频道 ID，用于认证连接建立在线订阅。 */
    List<Long> findChannelIdsByAccountId(long accountId);

    /**
     * 为授权判断直接读取数据库，绕过最终一致的展示缓存。
     * 返回当前事务可见的记录；不存在时为空，不提供检查与后续写入之间的并发保证。
     */
    Optional<ChannelMember> findByRelationForAuthorization(long channelId, long accountId);

    /**
     * 锁定并读取最新成员关系，不使用缓存或事务旧快照；不存在时返回空。
     * 必须在事务内先锁定所属频道，遵守频道在前、成员在后的治理锁顺序。
     */
    Optional<ChannelMember> findByRelationForUpdate(long channelId, long accountId);

    /**
     * 按频道与账号查询单条活跃成员关系。
     * 用途：普通展示读取；授权判断必须使用 findByRelationForAuthorization。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员关系，未命中时返回空 Optional
     */
    Optional<ChannelMember> findByRelation(long channelId, long accountId);

    /**
     * 查询频道下的全部活跃成员。
     * 说明：显式全量快照，供通知接收人与治理快照使用，不参与分页列表。
     *
     * @param channelId 频道 ID
     * @return 成员关系列表；频道无成员时返回空列表
     */
    List<ChannelMember> listByChannelId(long channelId);

    /**
     * 分页查询账号参与的全部频道成员关系。
     * 输出：按频道 ID 倒序返回，供频道列表分页使用。
     *
     * @param accountId 账户 ID
     * @param page 分页参数
     * @return 分页成员关系
     */
    PageResult<ChannelMember> pageByAccountId(long accountId, PageQuery page);

    /**
     * 分页查询频道下的活跃成员。
     * 输出：按成员关系 ID 倒序返回，供成员列表分页使用。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页成员关系
     */
    PageResult<ChannelMember> pageByChannelId(long channelId, PageQuery page);

    /**
     * 保存新的成员关系。
     * 约束：同一 (频道, 账号) 只应存在一条记录，调用方需先确认不存在再写入。
     *
     * @param channelMember 待保存成员关系
     */
    void save(ChannelMember channelMember);

    /**
     * 更新已存在的成员关系。
     * 约束：按成员关系主键更新，调用方必须传入保留原主键的成员模型；
     * 治理写入需持有频道锁，并检查更新结果；未命中时由调用方拒绝并回滚事务。
     *
     * @param channelMember 待更新成员关系
     * @return 恰好更新一条成员关系时为 true；未命中时为 false
     */
    boolean update(ChannelMember channelMember);

    /**
     * 按成员关系主键删除单条成员关系。
     * 注意：入参是成员关系主键而非账号 ID，移除成员请传 ChannelMember.id。
     *
     * @param id 成员关系主键
     */
    void delete(long id);

    /**
     * 删除频道下的全部成员关系。
     * 默认实现保持内存替身兼容，持久化适配器应覆盖为集合级删除。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /** 发送事务持有频道锁时，取候选账号中当前仍属该频道的成员 ID，去重并按 ID 升序返回。 */
    List<Long> findMemberAccountIds(long channelId, List<Long> candidates);
}
