package team.carrypigeon.backend.chat.domain.features.server.application.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerSummary;

/**
 * 服务基础应用服务。
 * 职责：为协议层提供当前服务入口的最小用例编排。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerApplicationService {

    /**
     * 返回当前协议骨架的最小服务概览。
     *
     * @return 服务概览数据
     */
    public ServerSummary getSummary() {
        return new ServerSummary("carry-pigeon-backend", "UP", "READY");
    }
}
