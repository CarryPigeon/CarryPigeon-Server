package team.carrypigeon.backend.api.chat.domain.controller;

import team.carrypigeon.backend.api.connection.pool.CPChannel;

/**
 * 客户端发送信息分发器，用于将用户的数据分发到每个具体的controller中
 * */
public interface CPControllerDispatcher {
    void process(CPChannel channel,String msg);
}
