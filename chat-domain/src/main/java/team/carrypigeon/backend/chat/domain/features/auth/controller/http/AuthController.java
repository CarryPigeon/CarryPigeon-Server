package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.http.ResponseEntity;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.AuthSessionTokenResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.CreateTokenSessionRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RefreshAccessTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RevokeRefreshTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 鉴权 HTTP 入口。
 * 职责：承接邮箱验证码、会话令牌签发、刷新与撤销等 v1 公共鉴权协议请求。
 * 边界：当前阶段不扩展 OAuth、SSO、复杂权限与主业务资源接口。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证与会话", description = "邮箱验证码、会话令牌签发、刷新与撤销。")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final ServerApplicationService serverApplicationService;

    public AuthController(
            AuthApplicationService authApplicationService,
            ServerApplicationService serverApplicationService
    ) {
        this.authApplicationService = authApplicationService;
        this.serverApplicationService = serverApplicationService;
    }

    /**
     * 发送邮箱验证码。
     *
     * @param request 验证码请求
     * @return HTTP 204
     */
    @PostMapping("/email_codes")
    @Operation(summary = "发送邮箱验证码", description = "为目标邮箱签发一次性验证码。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "验证码请求体。当前只承载邮箱字段。", required = true,
            content = @Content(schema = @Schema(implementation = SendEmailCodeRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "验证码请求受理成功")
    })
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        authApplicationService.sendEmailCode(new SendEmailCodeCommand(request.email()));
        return ResponseEntity.noContent().build();
    }

    /**
     * 创建会话并签发 token。
     *
     * @param request 会话创建请求
     * @return v1 会话令牌响应
     */
    @PostMapping("/tokens")
    @Operation(summary = "创建会话并签发 token", description = "使用邮箱验证码创建会话；首次邮箱登录时视为注册。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "会话创建请求体。当前仅支持 `email_code` 授权类型，并要求提供客户端设备与插件安装态。", required = true,
            content = @Content(schema = @Schema(implementation = CreateTokenSessionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回会话令牌结果；required gate 不满足时返回 412")
    })
    public AuthSessionTokenResponse createTokenSession(@Valid @RequestBody CreateTokenSessionRequest request) {
        List<String> missingPlugins = serverApplicationService.findMissingRequiredPlugins(
                request.client().installedPlugins() == null
                        ? List.of()
                        : request.client().installedPlugins().stream().map(CreateTokenSessionRequest.InstalledPluginRequest::pluginId).toList()
        );
        if (!missingPlugins.isEmpty()) {
            throw ProblemException.validationFailed(
                    "required_plugin_missing",
                    "required plugins are missing",
                    java.util.Map.of("missing_plugins", missingPlugins)
            );
        }
        AuthSessionTokenResult result = authApplicationService.createTokenSession(
                new CreateTokenSessionCommand(request.grantType(), request.email(), request.code())
        );
        return toSessionTokenResponse(result);
    }

    /**
     * 使用 refresh token 刷新 access token。
     *
     * @param request 刷新请求
     * @return v1 会话令牌响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "使用 refresh token 刷新 access token，可同时轮换 refresh token。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "刷新请求体。包含 refresh token 与最小客户端设备信息。", required = true,
            content = @Content(schema = @Schema(implementation = RefreshAccessTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回新的会话令牌结果")
    })
    public AuthSessionTokenResponse refresh(@Valid @RequestBody RefreshAccessTokenRequest request) {
        AuthSessionTokenResult result = authApplicationService.refreshTokenSession(new RefreshTokenCommand(request.refreshToken()));
        return toSessionTokenResponse(result);
    }

    /**
     * 撤销 refresh token。
     *
     * @param request 注销请求
     * @return HTTP 204
     */
    @PostMapping("/revoke")
    @Operation(summary = "撤销 refresh token", description = "撤销指定 refresh token 对应的 refresh session。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "撤销请求体。当前仅要求 refresh token 与设备信息。", required = true,
            content = @Content(schema = @Schema(implementation = RevokeRefreshTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "撤销成功")
    })
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeRefreshTokenRequest request) {
        authApplicationService.logout(new LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    private AuthSessionTokenResponse toSessionTokenResponse(AuthSessionTokenResult result) {
        return new AuthSessionTokenResponse(
                "Bearer",
                result.accessToken(),
                result.expiresIn(),
                result.refreshToken(),
                Ids.toString(result.accountId()),
                result.newUser()
        );
    }
}
