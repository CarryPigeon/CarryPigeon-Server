package team.carrypigeon.backend.api.dao.database.channel.application;

import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;

/**
 * 频道申请数据访问接口
 * @author carrypigeon
 */
public interface ChannelApplicationDAO {
    /**
     * 通过id获取频道申请数据
     * @param id 申请信息id
     * */
    CPChannelApplication getById(long id);
    /**
     * 通过用户id获取用户申请信息
     * @param uid 用户id
     * @param cid 通道id
     * */
    CPChannelApplication getByUidAndCid(long uid, long cid);

    /**
     * 获取通道申请数据
     * @param cid 通道id
     * @param page 页码
     * @param pageSize 每页数量
     * */
    CPChannelApplication[] getByCid(long cid, int page, int pageSize);

    /**
     * 保存通道申请数据（有则为更新，无则为插入）
     * @param channelApplication 通道申请数据
     * */
    boolean save(CPChannelApplication channelApplication);


}