package team.carrypigeon.backend.api.dao.database.message;

import team.carrypigeon.backend.api.bo.domain.message.CPMessage;

import java.time.LocalDateTime;

/**
 * 消息dao接口
 * @author midreamsheep
 * */
public interface MessageDao {
    /**
     * 通过id获取消息
     * @param id 消息id
     * */
    CPMessage getById(long id);
    /**
     * 获取指定通道指定时间之前的count条**有效**消息 <br/>
     * 若总消息数量不足count则返回所有有效消息
     * @param cid 通道id
     * @param time 时间
     * @param count 获取数量，数量范围为[1,100]
     * */
    CPMessage[] getBefore(long cid, LocalDateTime time, int count);

    /**
     * 保存消息（已存在则为更新，不存在则为插入）
     * @param message 消息
     * */
    boolean save(CPMessage message);
}