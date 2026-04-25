package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.CurrentPresenceResult;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.ServerEchoRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 服务基础 HTTP 入口。
 * 职责：为系统提供最小回显和认证 presence 查询入口。
 * 边界：这里不是服务端匿名公开源契约入口，`/.well-known/carrypigeon-server` 才是唯一权威发现协议来源。
 */
@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final ServerApplicationService serverApplicationService;
    private final AuthRequestContext authRequestContext;

    public ServerController(ServerApplicationService serverApplicationService, AuthRequestContext authRequestContext) {
        this.serverApplicationService = serverApplicationService;
        this.authRequestContext = authRequestContext;
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

    /**
     * 查询当前认证账户在本服务节点上的 presence 状态。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的当前 presence 结果
     */
    @GetMapping("/presence/me")
    public CPResponse<CurrentPresenceResult> currentPresence(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        return CPResponse.success(serverApplicationService.getCurrentPresence(principal.accountId()));
    }
}
