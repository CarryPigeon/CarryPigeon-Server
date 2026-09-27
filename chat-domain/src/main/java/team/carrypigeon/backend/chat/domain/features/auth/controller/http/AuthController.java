package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.*;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.*;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

import java.util.Locale;

/**
 * 鉴权 HTTP 入口。
 * 职责：承接账号注册、会话令牌签发、刷新与撤销等 v1 公共鉴权协议请求。
 * 边界：当前阶段不扩展 OAuth、SSO、复杂权限与主业务资源接口。
 */
@RestController
@RequestMapping("/api/auth")
@PermitAll
@Tag(name = "认证与会话", description = "邮箱验证码、会话令牌签发、刷新与撤销。")
public class AuthController {

    private final AuthAccountApi authAccountDomainApi;
    private final AuthSessionApi authSessionDomainApi;
    private final AuthAccountApi authAccountApi;

    /**
     * 创建鉴权 HTTP 入口。
     *
     * @param authAccountDomainApi 鉴权账号领域 API
     * @param authSessionDomainApi 鉴权会话领域 API
     */
    public AuthController(
        AuthAccountApi authAccountDomainApi,
        AuthSessionApi authSessionDomainApi,
        AuthAccountApi authAccountApi
    ) {
        this.authAccountDomainApi = authAccountDomainApi;
        this.authSessionDomainApi = authSessionDomainApi;
        this.authAccountApi = authAccountApi;
    }

    /**
     * 用户名密码注册。
     *
     * @param request 注册请求
     * @return 注册成功结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用用户名邮箱以及密码创建新账户。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResult result = authAccountDomainApi.register(
            new RegisterCommand(request.username().trim(), request.password(),request.email(),request.code())
        );
        return ResponseEntity.status(201).body(new RegisterResponse(IdUtil.toString(result.accountId()), result.username()));
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
        AuthTokenResult result = authSessionDomainApi.login(
            new LoginCommand(request.username().trim(), request.password())
        );
        return toSessionTokenResponse(result);
    }

    /**
     * 创建会话并签发 token。
     *
     * @param request 会话创建请求
     * @return v1 会话令牌响应
     */
    @PostMapping("/tokens")
    @Operation(summary = "创建会话并签发 token", description = "使用邮箱验证码创建会话")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "会话创建请求体。当前仅支持 `email_code` 授权类型，并要求提供客户端上下文；device_id 为可选字段。", required = true,
            content = @Content(schema = @Schema(implementation = CreateTokenSessionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回会话令牌结果；required gate 不满足时返回 412")
    })
    public AuthSessionTokenResponse createTokenSession(@Valid @RequestBody CreateTokenSessionRequest request) {
        AuthSessionTokenResult result = authSessionDomainApi.createTokenSession(
                new CreateTokenSessionCommand(request.grantType(), request.email().trim().toLowerCase(), request.code())
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
        AuthSessionTokenResult result = authSessionDomainApi.refreshTokenSession(
            new RefreshTokenCommand(request.refreshToken().trim())
        );
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "撤销请求体。当前仅要求 refresh token。", required = true,
            content = @Content(schema = @Schema(implementation = RevokeRefreshTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "撤销成功")
    })
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeRefreshTokenRequest request) {
        authSessionDomainApi.logout(
            new LogoutCommand(request.refreshToken())
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新用户邮箱
     *
     * @param request 用于获取当前用户的request
     * @param body 具体的参数
     * */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/email")
    @Operation(summary = "更新当前用户邮箱", description = "使用验证码更新当前登录账户邮箱。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "邮箱更新成功")})
    public ResponseEntity<Void> updateEmail(
        HttpServletRequest request,
        @AuthenticationPrincipal CpPrincipal cpPrincipal,
        @Valid @RequestBody UpdateCurrentAccountEmailRequest body
    ) {
        // 更新数据
        authAccountApi.updateCurrentAccountEmail(new UpdateCurrentAccountEmailCommand(
            cpPrincipal.accountId(),
            body.email().trim().toLowerCase(Locale.ROOT),
            body.code()
        ));
        // 响应
        return ResponseEntity.noContent().build();
    }

    private AuthSessionTokenResponse toSessionTokenResponse(AuthSessionTokenResult result) {
        return new AuthSessionTokenResponse(
                "Bearer",
                result.accessToken(),
                result.expiresIn(),
                result.refreshToken(),
                IdUtil.toString(result.accountId())
        );
    }

    private AuthSessionTokenResponse toSessionTokenResponse(AuthTokenResult result) {
        return new AuthSessionTokenResponse(
                "Bearer",
                result.accessToken(),
                result.expiresIn(),
                result.refreshToken(),
                IdUtil.toString(result.accountId())
        );
    }
}
