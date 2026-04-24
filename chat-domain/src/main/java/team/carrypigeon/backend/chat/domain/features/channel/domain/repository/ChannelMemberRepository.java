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
     * 判断账户是否属于频道成员。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 属于成员时返回 true
     */
    boolean exists(long channelId, long accountId);

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
    default Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
        return Optional.empty();
    }

    /**
     * 更新已存在的活跃成员投影。
     *
     * @param channelMember 待更新成员
     */
    default void update(ChannelMember channelMember) {
        throw new UnsupportedOperationException("channel member update is not supported");
    }

    /**
     * 删除已存在的活跃成员投影。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     */
    default void delete(long channelId, long accountId) {
        throw new UnsupportedOperationException("channel member delete is not supported");
    }

    /**
     * 查询频道下的全部活跃成员投影。
     *
     * @param channelId 频道 ID
     * @return 活跃成员列表
     */
    default List<ChannelMember> findByChannelId(long channelId) {
        return List.of();
    }

    /**
     * 查询频道下的全部成员账户 ID。
     *
     * @param channelId 频道 ID
     * @return 成员账户 ID 列表
     */
    List<Long> findAccountIdsByChannelId(long channelId);
}
