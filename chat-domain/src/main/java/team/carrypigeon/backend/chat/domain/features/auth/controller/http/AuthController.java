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
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.AuthSessionTokenResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.CreateTokenSessionRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.LoginRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RefreshAccessTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RegisterRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RegisterResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RevokeRefreshTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
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

    private final AuthAccountApi authAccountDomainApi;
    private final AuthSessionApi authSessionDomainApi;
    private final ServerEntranceApi serverEntranceDomainApi;
    private final AuthTokenSettings authTokenSettings;

    /**
     * 创建鉴权 HTTP 入口。
     *
     * @param authAccountDomainApi 鉴权账号领域 API
     * @param authSessionDomainApi 鉴权会话领域 API
     * @param serverEntranceDomainApi 服务入口领域 API
     * @param authTokenSettings token 展示用配置
     */
    public AuthController(
            AuthAccountApi authAccountDomainApi,
            AuthSessionApi authSessionDomainApi,
            ServerEntranceApi serverEntranceDomainApi,
            AuthTokenSettings authTokenSettings
    ) {
        this.authAccountDomainApi = authAccountDomainApi;
        this.authSessionDomainApi = authSessionDomainApi;
        this.serverEntranceDomainApi = serverEntranceDomainApi;
        this.authTokenSettings = authTokenSettings;
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
        authAccountDomainApi.sendEmailCode(new SendEmailCodeCommand(request.email()));
        return ResponseEntity.noContent().build();
    }

    /**
     * 用户名密码注册。
     *
     * @param request 注册请求
     * @return 注册成功结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户名密码注册", description = "使用用户名密码创建新账户。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResult result = authAccountDomainApi.register(new RegisterCommand(request.username().trim(), request.password()));
        return ResponseEntity.status(201).body(new RegisterResponse(Ids.toString(result.accountId()), result.username()));
    }

    /**
     * 用户名密码登录。
     *
     * @param request 登录请求
     * @return 会话令牌响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户名密码登录", description = "使用用户名密码创建会话并签发 token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回会话令牌结果")
    })
    public AuthSessionTokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResult result = authSessionDomainApi.login(new LoginCommand(request.username().trim(), request.password()));
        return toSessionTokenResponse(result);
    }

    /**
     * 创建会话并签发 token。
     *
     * @param request 会话创建请求
     * @return v1 会话令牌响应
     */
    @PostMapping("/tokens")
    @Operation(summary = "创建会话并签发 token", description = "使用邮箱验证码创建会话；首次邮箱登录时视为注册。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "会话创建请求体。当前仅支持 `email_code` 授权类型，并要求提供客户端上下文；device_id 为可选字段。", required = true,
            content = @Content(schema = @Schema(implementation = CreateTokenSessionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回会话令牌结果；required gate 不满足时返回 412")
    })
    public AuthSessionTokenResponse createTokenSession(@Valid @RequestBody CreateTokenSessionRequest request) {
        List<String> missingPlugins = serverEntranceDomainApi.findMissingRequiredPlugins(
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
        AuthSessionTokenResult result = authSessionDomainApi.createTokenSession(
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "刷新请求体。包含 refresh token 与可选客户端上下文。", required = true,
            content = @Content(schema = @Schema(implementation = RefreshAccessTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回新的会话令牌结果")
    })
    public AuthSessionTokenResponse refresh(@Valid @RequestBody RefreshAccessTokenRequest request) {
        AuthSessionTokenResult result = authSessionDomainApi.refreshTokenSession(new RefreshTokenCommand(request.refreshToken()));
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "撤销请求体。当前仅要求 refresh token；client 为兼容上下文。", required = true,
            content = @Content(schema = @Schema(implementation = RevokeRefreshTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "撤销成功")
    })
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeRefreshTokenRequest request) {
        authSessionDomainApi.logout(new LogoutCommand(request.refreshToken()));
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

    private AuthSessionTokenResponse toSessionTokenResponse(AuthTokenResult result) {
        return new AuthSessionTokenResponse(
                "Bearer",
                result.accessToken(),
                authTokenSettings.accessTokenTtl().toSeconds(),
                result.refreshToken(),
                Ids.toString(result.accountId()),
                false
        );
    }
}
