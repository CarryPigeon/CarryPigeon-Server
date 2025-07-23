package team.carrypigeon.backend.api.dao.message;

import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;

/**
 * 消息相关dao接口
 * */
public interface CPMessageDAO {
    /**
     * 存储消息
     * */
    boolean saveMessage(CPMessageBO message);
    /**
     * 删除消息
     * */
    boolean deleteMessage(long id,long userId);
    /**
     * 查询消息
     * */
    CPMessageBO getMessage(long id);

    /**
     * 获取指定时间之前的特定数量的消息
     * */
    Long[] getMessageFromTime(long channelId, long fromTime, int count);
}