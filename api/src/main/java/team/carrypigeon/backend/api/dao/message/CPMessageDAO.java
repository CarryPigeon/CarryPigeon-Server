package team.carrypigeon.backend.api.dao.message;

import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;

/**
 * 消息相关dao接口
 * */
public interface CPMessageDAO {
    /**
     * 发送消息
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
}