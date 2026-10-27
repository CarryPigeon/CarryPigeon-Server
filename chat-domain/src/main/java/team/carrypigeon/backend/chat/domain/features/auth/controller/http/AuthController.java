package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto.*;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AuthTokenResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

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

    public AuthController(
        AuthTokenApi authTokenApi,
        AuthAccountApi authAccountApi
    ) {
        this.authTokenApi = authTokenApi;
        this.authAccountApi = authAccountApi;
    }

    /**
     * 用户名密码注册<br/>
     * 注册需要邮箱进行验证<br/>
     * 用户名和邮箱都必须唯一的绑定到一个账户上</br>
     * 请求格式：
     * ```json
     * {
     *     "username": "string",
     *     "password": "string",
     *     "email": "string",
     *     "code": "string"
     * }
     * ```
     * <br/>
     * 登陆后响应用户id和用户名，用于用户登录时候
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用用户名邮箱以及密码创建新账户。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功")
    })
    public ResponseEntity<RegisterResponseBody> register(
        @Valid @RequestBody RegisterRequestBody request
    ) {
        RegisterResult result = authAccountApi.register(request.username().trim(), request.password(),request.email().toLowerCase(Locale.ROOT),request.code());
        return ResponseEntity.status(201).body(new RegisterResponseBody(IdUtil.toString(result.accountId()), result.username()));
    }

    /**
     * 用户名密码登录，可通过配置文件配置限制用户名密码登录<br/>
     * 参考请求：
     * ```json
     * {
     *     "username": "string",
     *     "password": "string"
     * }
     * ```
     * <br/>
     * 登陆后响应会话令牌，用于后续请求认证
     */
    @PostMapping("/login/pwd")
    @Operation(summary = "用户名密码登录", description = "使用用户名密码创建会话并签发 token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回会话令牌结果")
    })
    public ResponseEntity<AuthSessionTokenResponse> loginByPWD(@Valid @RequestBody PWDLoginRequestBody request) {
        AuthTokenResult result = authTokenApi.loginByPWD(request.username().trim(), request.password());
        return ResponseEntity.ok().body(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            IdUtil.toString(result.accountId())
        ));
    }

    /**
     * 邮箱验证码登录<br/>
     * 参考请求：
     * ```json
     * {
     *     "email": "string",
     *     "code": "string"
     * }
     * ```
     * <br/>
     * 登陆后响应会话令牌，用于后续请求认证
     */
    @PostMapping("/login/email")
    @Operation(summary = "邮箱验证码登录", description = "使用邮箱验证码创建会话并签发 token。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回会话令牌结果")
    })
    public ResponseEntity<AuthSessionTokenResponse> loginByEmail(
        @Valid @RequestBody EmailLoginRequestBody request
    ) {
        AuthTokenResult result = authTokenApi.loginByEmail(request.email(),request.code());
        return ResponseEntity.ok().body(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            IdUtil.toString(result.accountId())
        ));
    }

    /**
     * 使用 refresh token 刷新 access token<br/>
     * 示例请求：
     * ```json
     * {
     *     "refresh_token": "string"
     * }
     * ```
     * @return 会话令牌响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "使用 refresh token 刷新 access token，可同时轮换 refresh token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回新的会话令牌结果")
    })
    public ResponseEntity<AuthSessionTokenResponse> refresh(
        @Valid @RequestBody RefreshAccessTokenRequestBody request
    ) {
        AuthTokenResult result = authTokenApi.refreshToken(request.refreshToken().trim());
        return ResponseEntity.ok().body(new AuthSessionTokenResponse(
            "Bearer",
            result.accessToken(),
            result.expiresIn(),
            result.refreshToken(),
            IdUtil.toString(result.accountId())
        ));
    }

    /**
     * 撤销 refresh token<br/>
     * 参考请求：
     * ```json
     * {
     *     "refresh_token": "string"
     * }
     * ```
     */
    @PostMapping("/revoke")
    @Operation(summary = "撤销 refresh token", description = "撤销指定 refresh token 对应的 refresh session。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "撤销成功")
    })
    public ResponseEntity<Void> revoke(
        @Valid @RequestBody RevokeRefreshTokenRequestBody request
    ) {
        authTokenApi.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新用户邮箱<br/>
     * 参考请求：<br/>
     * ```json
     * {
     *     "email": "string",
     *     "code": "string"
     * }
     * ```
     * */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/email")
    @Operation(summary = "更新当前用户邮箱", description = "使用验证码更新当前登录账户邮箱。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "邮箱更新成功")})
    public ResponseEntity<Void> updateEmail(
        @AuthenticationPrincipal CpPrincipal cpPrincipal,
        @Valid @RequestBody UpdateCurrentAccountEmailRequest body
    ) {
        // 更新数据
        authAccountApi.updateEmail(cpPrincipal.accountId(), body.email().trim().toLowerCase(Locale.ROOT), body.code());
        // 响应
        return ResponseEntity.noContent().build();
    }

}
