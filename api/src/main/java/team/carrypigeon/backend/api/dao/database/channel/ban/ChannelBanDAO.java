package team.carrypigeon.backend.api.dao.database.channel.ban;

import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;

/**
 * 频道禁言（Ban）相关 DAO 接口。
 * <p>
 * 该接口定义禁言记录的查询、保存与删除能力；具体实现位于 {@code dao} 模块。
 */
public interface ChannelBanDAO {
    /**
     * 通过禁言记录 ID 获取禁言数据。
     *
     * @param id ban 信息 id
     */
    CPChannelBan getById(long id);

    /**
     * 获取频道的禁言数据列表。
     *
     * @param channelId 频道 id
     */
    CPChannelBan[] getByChannelId(long channelId);

    /**
     * 获取用户在频道内的禁言数据（如存在）。
     *
     * @param uid 用户 id
     * @param cid 频道 id
     */
    CPChannelBan getByChannelIdAndUserId(long uid, long cid);

    /**
     * 保存禁言数据（有则更新，无则插入）。
     *
     * @param channelBan 禁言数据
     */
    boolean save(CPChannelBan channelBan);

    /**
     * 删除禁言数据。
     *
     * @param channelBan 禁言数据
     */
    boolean delete(CPChannelBan channelBan);
}
