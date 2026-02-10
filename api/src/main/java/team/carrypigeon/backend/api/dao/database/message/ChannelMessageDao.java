package team.carrypigeon.backend.api.dao.database.message;

import team.carrypigeon.backend.api.bo.domain.message.CPMessage;

/**
 * 频道消息 DAO 接口。
 * <p>
 * 本接口以 `mid`（消息 ID）作为分页与游标基准，
 * 避免仅使用时间戳带来的边界重复/遗漏问题。
 */
public interface ChannelMessageDao {

    /**
     * 按消息 ID 查询。
     *
     * @param id 消息 ID
     * @return 消息实体，不存在时返回 null
     */
    CPMessage getById(long id);

    /**
     * 查询某频道在游标之前的消息（不含游标）。
     * <p>
     * 约定：
     * - `cursorMid <= 0` 视为首屏（使用最大游标）；
     * - 否则返回 `id < cursorMid` 的消息。
     *
     * @param cid       频道 ID
     * @param cursorMid 游标消息 ID（不包含）
     * @param count     期望返回条数
     * @return 消息数组
     */
    CPMessage[] listBefore(long cid, long cursorMid, int count);

    /**
     * 统计某频道在指定消息 ID 之后的消息数。
     *
     * @param cid      频道 ID
     * @param startMid 起始消息 ID（不包含）
     * @return 未读消息数量
     */
    int countAfter(long cid, long startMid);

    /**
     * 保存消息（新增或更新）。
     *
     * @param message 消息实体
     * @return 保存是否成功
     */
    boolean save(CPMessage message);

    /**
     * 删除消息。
     *
     * @param message 消息实体
     * @return 删除是否成功
     */
    boolean delete(CPMessage message);
}
