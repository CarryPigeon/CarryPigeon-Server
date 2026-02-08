package team.carrypigeon.backend.api.dao.database.channel.application;

import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;

/**
 * 频道申请数据访问接口。
 * <p>
 * 用于管理用户申请加入频道的记录（查询与保存）；具体实现位于 {@code dao} 模块。
 */
public interface ChannelApplicationDAO {
    /**
     * 通过申请记录 ID 获取频道申请数据。
     *
     * @param id 申请信息 id
     */
    CPChannelApplication getById(long id);

    /**
     * 通过用户 ID 与频道 ID 获取用户申请信息（如存在）。
     *
     * @param uid 用户 id
     * @param cid 频道 id
     */
    CPChannelApplication getByUidAndCid(long uid, long cid);

    /**
     * 分页获取频道申请数据。
     *
     * @param cid      频道 id
     * @param page     页码（从 1 开始或从 0 开始由实现约定）
     * @param pageSize 每页数量
     */
    CPChannelApplication[] getByCid(long cid, int page, int pageSize);

    /**
     * 保存频道申请数据（有则更新，无则插入）。
     *
     * @param channelApplication 频道申请数据
     */
    boolean save(CPChannelApplication channelApplication);
}
