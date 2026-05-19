package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "认证与会话", description = "注册、登录、令牌刷新、会话注销与当前登录用户查询。")
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
    @Operation(summary = "注册新账户", description = "使用用户名和密码注册新账户，成功后返回最小账户标识信息。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "注册请求体。当前只接受用户名与密码，不包含验证码或邀请信息。", required = true,
            content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；用户名或密码不合法时 `CPResponse.code=200`")
    })
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
    @Operation(summary = "使用密码登录", description = "使用用户名和密码登录，成功后返回 access token 与 refresh token。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "登录请求体。当前使用用户名 + 密码模式，不包含验证码或设备信息。", required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；凭证错误时 `CPResponse.code=300`；请求体不合法时 `CPResponse.code=200`")
    })
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
    @Operation(summary = "刷新访问令牌", description = "使用 refresh token 换取新的 access token 与 refresh token。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "刷新令牌请求体。提交 refresh token 后换取新的 token 对。", required = true,
            content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；refresh token 无效或过期时通常返回 `CPResponse.code=300`")
    })
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
    @Operation(summary = "注销当前会话", description = "撤销指定 refresh token 对应的 refresh session。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "注销请求体。当前无需 Bearer access token，直接提交待撤销的 refresh token。", required = true,
            content = @Content(schema = @Schema(implementation = LogoutRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "该接口在 HTTP 层无需 Bearer；业务成功时 `CPResponse.code=100`，refresh token 非法时通常返回 `CPResponse.code=300`")
    })
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
    @Operation(summary = "读取当前登录用户", description = "读取当前 access token 绑定的用户身份信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；缺少或非法 Bearer access token 时通常返回 `CPResponse.code=300`")
    })
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
