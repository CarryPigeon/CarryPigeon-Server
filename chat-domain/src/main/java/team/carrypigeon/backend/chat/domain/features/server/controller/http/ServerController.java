package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.http.Result;
import team.carrypigeon.backend.chat.domain.features.server.controller.http.dto.ServerDiscoveryResponse;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.ServerEntranceApi;

/**
 * 服务基础 HTTP 入口。
 * 职责：承接服务发现入口。
 */
@RestController
@RequestMapping("/api/server")
@Tag(name = "服务基础", description = "服务发现能力。")
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
     * 服务发现文档。
     *
     * <p>请求 {@code GET /api/server}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ServerDiscoveryResponse}。
     * <p>失败示例：内部错误 10012。
     */
    @PermitAll
    @GetMapping(produces = "application/json")
    @Operation(summary = "读取服务发现文档", description = "返回客户端握手与登录前置阶段所需的服务发现信息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回服务发现文档", useReturnTypeSchema = true))
    public Result<ServerDiscoveryResponse> getServerDiscoveryDocument() {
        return Result.success(ServerDiscoveryResponse.from(serverEntranceDomainApi.getServerDiscoveryDocument()));
    }


}
