package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;

/**
 * 服务基础 HTTP 入口。
 * 职责：承接服务发现与认证后的节点 presence 查询入口。
 * 边界：这里只承载 v1 公开入口协议，不扩展 WS 业务事件与主业务资源接口。
 */
@RestController
@RequestMapping("/api/server")
@Tag(name = "服务基础", description = "服务发现与当前节点在线状态查询能力。")
public class ServerController {

    private final ServerEntranceApi serverEntranceDomainApi;

    /**
     * 创建服务基础 HTTP 入口。
     *
     * @param serverEntranceDomainApi 服务入口领域 API
     */
    public ServerController(ServerEntranceApi serverEntranceDomainApi) {
        this.serverEntranceDomainApi = serverEntranceDomainApi;
    }

    /**
     * 返回 v1 服务发现文档。
     *
     * @return 客户端握手阶段的发现信息
     */
    @GetMapping
    @Operation(summary = "读取服务发现文档", description = "返回客户端握手与登录前置阶段所需的服务发现信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回服务发现文档")
    })
    public ServerDiscoveryDocument getServerDiscoveryDocument() {
        return serverEntranceDomainApi.getServerDiscoveryDocument();
    }
}
