package team.carrypigeon.backend.api.bo.domain;

import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;

/**
 * CarryPigeon通道包装类，用于包装通道用于不同模块之间的解耦
 * */
public interface CPChannel {
    /**
     * 通过通道发送消息
     * */
    void sendMessage(String msg);
    /**
     * 获取通道对应的user结构
     * */
    CPUserBO getCPUserBO();
    /**
     * 设置通道用户结构
     * */
    void setCPUserBO(CPUserBO cpUserBO);
}
