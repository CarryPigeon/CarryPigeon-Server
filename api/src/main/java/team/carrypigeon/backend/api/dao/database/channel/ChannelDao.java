package team.carrypigeon.backend.api.dao.database.channel;

import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;

/**
 * 通道数据访问接口
 * @author midreamsheep
 * */
public interface ChannelDao {
    /**
     * 通过id获取通道数据
     * @param id 通道id
     * */
    CPChannel getById(long id);
    /**
     * 获取所有固有通道：owner=-1
     * */
    CPChannel[] getAll();
    /**
     * 保存通道数据（已存在则为更新，不存在则为插入）
     * @param channel 通道数据
     * */
    boolean save(CPChannel channel);
}