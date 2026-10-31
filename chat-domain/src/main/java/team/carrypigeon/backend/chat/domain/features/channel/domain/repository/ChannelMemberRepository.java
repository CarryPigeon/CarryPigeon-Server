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

    /**
     * 按频道与账号查询单条活跃成员关系。
     * 用途：可见性判定、成员身份校验与治理用例中的操作者/目标读取。
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
     * 重新生成主键会导致更新命中不到任何行，从而静默丢弃角色变更。
     *
     * @param channelMember 待更新成员关系
     */
    void update(ChannelMember channelMember);

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
}
