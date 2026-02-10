package team.carrypigeon.backend.api.dao.database.channel.read;

import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;

/**
 * 频道已读状态 DAO 接口。
 * <p>
 * 每条记录表示“某用户在某频道”的已读进度。
 */
public interface ChannelReadStateDao {

    /**
     * 按主键查询已读状态。
     *
     * @param id 主键 ID
     * @return 已读状态，不存在时返回 null
     */
    CPChannelReadState getById(long id);

    /**
     * 按用户和频道查询已读状态。
     *
     * @param uid 用户 ID
     * @param cid 频道 ID
     * @return 已读状态，不存在时返回 null
     */
    CPChannelReadState getByUidAndCid(long uid, long cid);

    /**
     * 保存已读状态（新增或更新）。
     *
     * @param state 已读状态实体
     * @return 保存是否成功
     */
    boolean save(CPChannelReadState state);

    /**
     * 删除已读状态。
     *
     * @param state 已读状态实体
     * @return 删除是否成功
     */
    boolean delete(CPChannelReadState state);
}
