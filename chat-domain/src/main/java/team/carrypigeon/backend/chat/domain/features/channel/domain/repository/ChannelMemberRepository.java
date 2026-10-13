package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;

/**
 * 频道成员仓储抽象。
 * 职责：定义频道成员关系的业务语义读写入口。
 * 边界：不暴露数据库实现细节。
 */
public interface ChannelMemberRepository {

    /**
     * 保存频道成员关系。
     *
     * @param channelMember 成员关系
     */
    void save(ChannelMember channelMember);

    /**
     * 查询活跃成员投影。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回活跃成员
     */
    Optional<ChannelMember> findByRelation(long channelId, long accountId);

    /**
     * 更新已存在的活跃成员投影。
     *
     * @param channelMember 待更新成员
     */
    void update(ChannelMember channelMember);

    /**
     * 删除已存在的活跃成员投影。
     *
     * @param memberId 成员 ID
     */
    void delete(long memberId);

    /**
     * 删除频道下的全部成员关系。
     * 默认实现保持内存替身兼容，持久化适配器应覆盖为集合级删除。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /**
     * 查询频道下的全部活跃成员投影。
     *
     * @param channelId 频道 ID
     * @return 活跃成员列表
     */
    List<ChannelMember> findMembersByChannelId(long channelId);

    /**
     * 查询账户参与的全部频道成员。
     *
     * @param accountId 账户 ID
     * @return 频道列表
     */
    List<ChannelMember> findMembersByAccountId(long accountId);
}
