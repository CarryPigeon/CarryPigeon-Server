package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "服务基础", description = "基础服务探测、回显与当前节点在线状态查询能力。")
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
    @Operation(summary = "执行回显测试", description = "返回统一响应格式的原始输入内容，用于最小联通性验证。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100` 且 `data` 为原始输入字符串；参数为空时通常返回 `CPResponse.code=200`")
    })
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
    @Operation(summary = "读取当前节点在线状态", description = "读取当前认证账户在本服务节点上的在线状态。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；缺少或非法 access token 时通常返回 `CPResponse.code=300`")
    })
    public CPResponse<CurrentPresenceResult> currentPresence(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        return CPResponse.success(serverApplicationService.getCurrentPresence(principal.accountId()));
    }
}
