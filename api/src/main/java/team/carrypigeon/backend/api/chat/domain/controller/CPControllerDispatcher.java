package team.carrypigeon.backend.api.chat.domain.controller;

import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.common.response.CPResponse;

/**
 * 客户端发送信息分发器，用于将用户的数据分发到每个具体的controller中
 * */
public interface CPControllerDispatcher {
    CPResponse process(String msg, CPChannel channel);

    void channelInactive(CPChannel cpChannel);
}
