package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.RequiredGateCheckResult;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.RequiredGateCheckRequest;

/**
 * required gate HTTP 入口。
 * 职责：为匿名客户端提供安装态预检查入口。
 * 边界：这里只返回缺失插件列表，不负责会话签发。
 */
@RestController
@RequestMapping("/api/gates/required")
@Tag(name = "Required Gate", description = "登录前置 required 插件检查。")
public class ServerGateController {

    private final ServerApplicationService serverApplicationService;

    public ServerGateController(ServerApplicationService serverApplicationService) {
        this.serverApplicationService = serverApplicationService;
    }

    /**
     * 按客户端已安装插件列表执行 required gate 检查。
     *
     * @param request gate 检查请求
     * @return 缺失必需插件列表
     */
    @PostMapping("/check")
    @Operation(summary = "执行 required gate 预检查", description = "按当前设备已安装插件列表返回缺失的必需插件。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回当前缺失插件列表；空列表表示 gate 已满足")
    })
    public RequiredGateCheckResult check(@Valid @RequestBody RequiredGateCheckRequest request) {
        List<String> missingPlugins = serverApplicationService.findMissingRequiredPlugins(
                request.client().installedPlugins() == null
                        ? List.of()
                        : request.client().installedPlugins().stream().map(RequiredGateCheckRequest.InstalledPluginRequest::pluginId).toList()
        );
        return new RequiredGateCheckResult(missingPlugins);
    }
}
