package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.WellKnownServerDocument;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 服务端公开源信息 HTTP 入口。
 * 职责：提供 `/.well-known/carrypigeon-server` 的权威匿名可读公开源契约。
 * 边界：只承接协议层请求，不承载服务端元数据组装逻辑。
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "服务发现", description = "匿名可读的服务端公开源发现接口。")
public class ServerWellKnownController {

    private final ServerApplicationService serverApplicationService;

    public ServerWellKnownController(ServerApplicationService serverApplicationService) {
        this.serverApplicationService = serverApplicationService;
    }

    /**
     * 返回服务端公开源信息。
     *
     * @return 统一响应包装的最小公开源信息文档
     */
    @GetMapping("/carrypigeon-server")
    @Operation(summary = "读取公开源文档", description = "返回 `/.well-known/carrypigeon-server` 的权威匿名公开源文档。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；当前接口设计为匿名可读的服务发现入口")
    })
    public CPResponse<WellKnownServerDocument> getWellKnownServerDocument() {
        return CPResponse.success(serverApplicationService.getWellKnownServerDocument());
    }
}
