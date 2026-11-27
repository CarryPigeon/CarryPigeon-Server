package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import java.util.Locale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.http.address.ClientAddressResolver;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto.*;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AuthTokenResult;

/**
 * 账号和鉴权相关的api文档
 * 职责：只负责登陆注册、会话创建、token签收等相关功能
 */
@RestController
@RequestMapping("/api/auth")
@PermitAll
@Tag(name = "认证与会话", description = "邮箱验证码、会话令牌签发、刷新与撤销。")
public class AuthController {

    private final AuthTokenApi authTokenApi;
    private final AuthAccountApi authAccountApi;
    private final ClientAddressResolver clientAddressResolver;

    public AuthController(
        AuthTokenApi authTokenApi,
        AuthAccountApi authAccountApi,
        ClientAddressResolver clientAddressResolver
    ) {
        this.authTokenApi = authTokenApi;
        this.authAccountApi = authAccountApi;
        this.clientAddressResolver = clientAddressResolver;
    }

    /**
     * 用户名密码注册
     * 注册需要邮箱进行验证
     * 用户名和邮箱都必须唯一的绑定到一个账户上
     *
     * <p>请求 {@code POST /api/auth/register}。JSON 请求体示例 {@code {"username":"alice","email":"alice@example.com","password":"example-password","code":"123456"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code RegisterResponseBody}。
     * <p>失败示例：用户名已占用 11010，邮箱已占用 11011。
     */
    @PostMapping(value = "/register", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "用户注册", description = "使用用户名邮箱以及密码创建新账户。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "注册成功", useReturnTypeSchema = true))
    public Result register(
        @Valid
        @RequestBody
        RegisterRequestBody request
    ) {
        RegisterResult result = authAccountApi.register(request.username().trim(), request.password(),request.email().toLowerCase(Locale.ROOT),request.code());
        return Result.success(new RegisterResponseBody(result.accountId(), result.username()));
    }

    /**
     * 用户名密码登录，可通过配置文件配置限制用户名密码登录
     *
     * <p>请求 {@code POST /api/auth/login/pwd}。JSON 请求体示例 {@code {"username":"alice","password":"example-password"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code AuthSessionTokenResponse}。
     * <p>失败示例：凭据错误 11006，登录限流 11008。
     */
    @PostMapping(value = "/login/pwd", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "用户名密码登录", description = "使用用户名密码创建会话并签发 token。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回会话令牌结果", useReturnTypeSchema = true))
    public Result loginByPWD(
        @Valid
        @RequestBody
        PWDLoginRequestBody request,
        HttpServletRequest httpRequest
    ) {
        AuthTokenResult result = authTokenApi.loginByPWD(request.username().trim(), request.password(),
                clientAddressResolver.resolve(httpRequest));
        return Result.success(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            result.accountId()
        ));
    }

    /**
     * 邮箱验证码登录
     *
     * <p>请求 {@code POST /api/auth/login/email}。JSON 请求体示例 {@code {"email":"alice@example.com","code":"123456"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code AuthSessionTokenResponse}。
     * <p>失败示例：验证码无效 12001。
     */
    @PostMapping(value = "/login/email", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "邮箱验证码登录", description = "使用邮箱验证码创建会话并签发 token。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回会话令牌结果", useReturnTypeSchema = true))
    public Result loginByEmail(
        @Valid
        @RequestBody
        EmailLoginRequestBody request
    ) {
        AuthTokenResult result = authTokenApi.loginByEmail(request.email().trim().toLowerCase(Locale.ROOT),request.code());
        return Result.success(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            result.accountId()
        ));
    }

    /**
     * 使用 refresh token 刷新 access token
     *
     * <p>请求 {@code POST /api/auth/refresh}。JSON 请求体示例 {@code {"refresh_token":"example-refresh-token"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code AuthSessionTokenResponse}。
     * <p>失败示例：刷新令牌无效 11004。
     */
    @PostMapping(value = "/refresh", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "刷新访问令牌", description = "使用 refresh token 刷新 access token，可同时轮换 refresh token。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回新的会话令牌结果", useReturnTypeSchema = true))
    public Result refresh(
        @Valid
        @RequestBody
        RefreshAccessTokenRequestBody request
    ) {
        AuthTokenResult result = authTokenApi.refreshToken(request.refreshToken().trim());
        return Result.success(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            result.accountId()
        ));
    }

    /**
     * 撤销 refresh token
     *
     * <p>请求 {@code POST /api/auth/revoke}。JSON 请求体示例 {@code {"refresh_token":"example-refresh-token"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：刷新令牌无效 11004。
     */
    @PostMapping(value = "/revoke", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "撤销 refresh token", description = "撤销指定 refresh token 对应的 refresh session。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result revoke(
        @Valid
        @RequestBody
        RevokeRefreshTokenRequestBody request
    ) {
        authTokenApi.logout(request.refreshToken());
        return Result.success();
    }

    /**
     * 更新用户邮箱
     *
     * <p>请求 {@code PUT /api/auth/me/email}。JSON 请求体示例 {@code {"email":"alice@example.com","code":"123456"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：邮箱已占用 11011。
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping(value = "/me/email", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "更新当前用户邮箱", description = "使用验证码更新当前登录账户邮箱。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result updateEmail(
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal,
        @Valid
        @RequestBody
        UpdateCurrentAccountEmailRequest body
    ) {
        // 更新数据
        authAccountApi.updateEmail(cpPrincipal.accountId(), body.email().trim().toLowerCase(Locale.ROOT), body.code());
        // 响应
        return Result.success();
    }

    /**
     * 更新用户密码
     * <p>请求 {@code PUT /api/auth/me/password}。JSON 请求体示例 {@code {"password":"example-password","code":"123456"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：密码错误 11002，验证码无效 12001。
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping(value = "/me/password", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "更新当前用户密码", description = "使用验证码更新当前登录账户密码。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result updatePassword(
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal,
        @Valid
        @RequestBody
        UpdateCurrentAccountPasswordRequest body
    ) {
        // 更新数据
        authAccountApi.updatePassword(cpPrincipal.accountId(), body.password(), body.code());
        // 响应
        return Result.success();
    }
}
