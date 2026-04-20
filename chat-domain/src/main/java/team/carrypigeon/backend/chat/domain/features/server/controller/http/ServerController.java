package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerSummary;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.ServerEchoRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 服务基础 HTTP 入口。
 * 职责：为系统提供最小健康检查和回显入口。
 * 边界：当前阶段只承接协议层基础请求，不承载聊天业务规则。
 */
@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final ServerApplicationService serverApplicationService;

    public ServerController(ServerApplicationService serverApplicationService) {
        this.serverApplicationService = serverApplicationService;
    }

    /**
     * 返回最小服务概览。
     *
     * @return 统一响应包装的服务概览
     */
    @GetMapping("/summary")
    public CPResponse<ServerSummary> summary() {
        return CPResponse.success(serverApplicationService.getSummary());
    }

    /**
     * 返回统一响应格式的回显结果。
     *
     * @param request 回显请求
     * @return 统一响应包装的原始输入
     */
    @PostMapping("/echo")
    public CPResponse<String> echo(@Valid @RequestBody ServerEchoRequest request) {
        return CPResponse.success(request.content());
    }
}
