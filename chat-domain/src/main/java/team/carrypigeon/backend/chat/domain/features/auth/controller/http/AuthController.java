package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.CurrentUserResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.AuthTokenResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.CurrentUserResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.LoginRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.LogoutRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RefreshTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RegisterRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RegisterResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 鉴权 HTTP 入口。
 * 职责：承接注册、登录、刷新和注销协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露验证码、OAuth、SSO 或复杂权限逻辑。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final AuthRequestContext authRequestContext;

    public AuthController(AuthApplicationService authApplicationService, AuthRequestContext authRequestContext) {
        this.authApplicationService = authApplicationService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 注册新账户。
     *
     * @param request 注册请求
     * @return 统一响应包装的注册结果
     */
    @PostMapping("/register")
    public CPResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResult result = authApplicationService.register(new RegisterCommand(request.username(), request.password()));
        return CPResponse.success(new RegisterResponse(result.accountId(), result.username()));
    }

    /**
     * 使用用户名密码登录。
     *
     * @param request 登录请求
     * @return 统一响应包装的登录结果
     */
    @PostMapping("/login")
    public CPResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResult result = authApplicationService.login(new LoginCommand(request.username(), request.password()));
        return CPResponse.success(toTokenResponse(result));
    }

    /**
     * 使用 refresh token 刷新令牌对。
     *
     * @param request 刷新请求
     * @return 统一响应包装的新令牌结果
     */
    @PostMapping("/refresh")
    public CPResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokenResult result = authApplicationService.refresh(new RefreshTokenCommand(request.refreshToken()));
        return CPResponse.success(toTokenResponse(result));
    }

    /**
     * 注销并撤销 refresh session。
     *
     * @param request 注销请求
     * @return 统一成功响应
     */
    @PostMapping("/logout")
    public CPResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(new LogoutCommand(request.refreshToken()));
        return CPResponse.success(null);
    }

    /**
     * 查询当前 access token 对应的用户。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的当前用户信息
     */
    @GetMapping("/me")
    public CPResponse<CurrentUserResponse> me(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        CurrentUserResult result = new CurrentUserResult(principal.accountId(), principal.username());
        return CPResponse.success(new CurrentUserResponse(result.accountId(), result.username()));
    }

    private AuthTokenResponse toTokenResponse(AuthTokenResult result) {
        return new AuthTokenResponse(
                result.accountId(),
                result.username(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt()
        );
    }
}
