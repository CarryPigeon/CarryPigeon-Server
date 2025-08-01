package team.carrypigeon.backend.api.dao.group;

import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;

/**
 * 群聊相关DAO接口
 * */
public interface CPGroupDAO {
    /**
     * 通过id获取群聊结构
     * */
    CPGroupBO getById(long id);
    /**
     * 获取所有固有群聊
     * */
    CPGroupBO[] getFixedGroups();
    /**
     * 创建群聊
     * */
    boolean createGroup(CPGroupBO group);
    /**
     * 删除群聊
     * */
    boolean deleteGroup(long id);
    /**
     * 更新群聊信息
     * */
    boolean updateGroup(CPGroupBO group);
}
