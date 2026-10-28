package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道禁言仓储抽象。
 * 职责：以业务语义表达频道禁言记录的读写入口。
 * 边界：不暴露数据库实现细节，不与活跃成员投影混用；"禁言是否仍然生效"的判定不属于本接口职责。
 */
public interface ChannelBanRepository {

    /**
     * 按频道与账号查询禁言记录。
     * 说明：返回的是该 (频道, 账号) 的唯一记录，可能已解除或已过期，是否生效由调用方按 revokedAt 与 expiresAt 判定。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被禁言账户 ID
     * @return 命中时返回禁言记录，未命中时返回空 Optional
     */
    Optional<ChannelBan> findByRelation(long channelId, long bannedAccountId);

    /**
     * 查询频道下的全部禁言记录。
     * 说明：包含已解除与已过期的历史记录，不做生效性过滤。
     *
     * @param channelId 频道 ID
     * @return 禁言记录列表；频道无禁言时返回空列表
     */
    List<ChannelBan> list(long channelId);

    /**
     * 保存新的禁言记录。
     * 约束：调用方负责生成 banId，本方法不做去重。
     *
     * @param channelBan 待保存禁言记录
     */
    void save(ChannelBan channelBan);

    /**
     * 更新已存在的禁言记录。
     * 约束：按 banId 更新，调用方必须沿用既有 banId；重新发号会导致更新命中不到任何行。
     *
     * @param channelBan 待更新禁言记录
     */
    void update(ChannelBan channelBan);

    /**
     * 删除禁言记录。
     * 约束：按 banId 删除，调用方必须沿用既有 banId；重新发号会导致删除命中不到任何行。
     *
     * @param banId 禁言记录 ID
     */
    void delete(long banId);
}
