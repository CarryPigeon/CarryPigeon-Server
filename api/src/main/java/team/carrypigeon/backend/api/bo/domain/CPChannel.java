package team.carrypigeon.backend.api.bo.domain;

import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;

/**
 * CarryPigeon通道包装类，用于包装通道用于不同模块之间的解耦
 * */
public interface CPChannel {
    void sendMessage(String msg);
    CPUserBO getCPUserBO();
    void setCPUserBO(CPUserBO cpUserBO);
}
