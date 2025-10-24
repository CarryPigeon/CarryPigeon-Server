package team.carrypigeon.backend.api.chat.domain.controller;

import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

/**
 * 客户端发送信息分发器，用于将用户的数据分发到每个具体的controller中
 * */
public interface CPControllerDispatcher {
    /**
     * 处理一个请求消息
     * @param msg 用户发送的消息
     * @param session 当前用户所在的channel
     * @return 根据规范，任何请求都需要返回一个返回值
     * */
    CPResponse process(String msg, CPSession session);

    /**
     * 当一个channel断开连接时，会调用这个方法用于进行数据清理
     * @param session 断开的channel
     * */
    void channelInactive(CPSession session);
}
