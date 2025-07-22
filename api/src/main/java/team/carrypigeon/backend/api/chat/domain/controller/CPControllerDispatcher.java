package team.carrypigeon.backend.api.chat.domain.controller;

import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

/**
 * 客户端发送信息分发器，用于将用户的数据分发到每个具体的controller中
 * */
public interface CPControllerDispatcher {
    /**
     * 处理一个请求消息
     * @param msg 用户发送的消息
     * @param channel 当前用户所在的channel
     * @return 根据规范，任何请求都需要返回一个返回值
     * */
    CPResponse process(String msg, CPChannel channel);

    /**
     * 当一个channel断开连接时，会调用这个方法用于进行数据清理
     * @param cpChannel 断开的channel
     * */
    void channelInactive(CPChannel cpChannel);
}
