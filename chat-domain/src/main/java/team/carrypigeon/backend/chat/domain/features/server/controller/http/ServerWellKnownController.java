package team.carrypigeon.backend.chat.domain.features.server.controller.http;

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
    public CPResponse<WellKnownServerDocument> getWellKnownServerDocument() {
        return CPResponse.success(serverApplicationService.getWellKnownServerDocument());
    }
}
