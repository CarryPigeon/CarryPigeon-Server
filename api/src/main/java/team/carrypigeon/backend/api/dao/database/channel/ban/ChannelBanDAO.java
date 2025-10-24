package team.carrypigeon.backend.api.dao.database.channel.ban;

import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;

public interface ChannelBanDAO {
    /**
     * 通过id获取频道封禁数据
     * @param id ban信息id
     * */
    CPChannelBan getById(long id);
    /**
     * 获取通道的封禁数据列表
     * @param channelId 通道id
     * */
    CPChannelBan[] getByChannelId(long channelId);
    /**
     * 获取用户在通道的封禁数据列表
     * @param channelId 通道id
     * @param userId 用户id
     * */
    CPChannelBan getByChannelIdAndUserId(long channelId, long userId);
    /**
     * 保存封禁数据（有则为更新，无则为插入）
     * @param channelBan 封禁数据
     * */
    boolean save(CPChannelBan channelBan);
    /**
     * 删除封禁数据
     * @param channelBan 封禁数据
     * */
    boolean delete(CPChannelBan channelBan);
}
