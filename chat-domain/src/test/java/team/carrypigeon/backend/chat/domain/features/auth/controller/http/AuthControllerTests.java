package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.LoginResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 协议测试。
 * 职责：验证注册、登录、刷新与注销入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库与密码哈希实现，只验证协议层请求到响应的稳定行为。
 */
class AuthControllerTests {

    private AuthApplicationService authApplicationService;

    private AuthRequestContext authRequestContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authApplicationService = mock(AuthApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证注册成功时返回统一成功响应与账户标识。
     */
    @Test
    @DisplayName("register success returns code 100")
    void register_success_returnsCode100() throws Exception {
        when(authApplicationService.register(any())).thenReturn(new RegisterResult(1001L, "carry-user"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1001L))
                .andExpect(jsonPath("$.data.username").value("carry-user"));
    }

    /**
     * 验证注册请求参数不合法时返回 200 响应码。
     */
    @Test
    @DisplayName("register invalid request returns code 200")
    void register_invalidRequest_returnsCode200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab","password":"123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证用户名重复问题会映射到 200 响应码。
     */
    @Test
    @DisplayName("register duplicate username returns code 200")
    void register_duplicateUsername_returnsCode200() throws Exception {
        when(authApplicationService.register(any()))
                .thenThrow(ProblemException.validationFailed("username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("username already exists"));
    }

    /**
     * 验证未预期异常会映射到 500 响应码。
     */
    @Test
    @DisplayName("register unexpected failure returns code 500")
    void register_unexpectedFailure_returnsCode500() throws Exception {
        when(authApplicationService.register(any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("internal server error"));
    }

    /**
     * 验证登录成功时返回统一成功响应与账户标识。
     */
    @Test
    @DisplayName("login success returns code 100")
    void login_success_returnsCode100() throws Exception {
        when(authApplicationService.login(any())).thenReturn(tokenResult());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1001L))
                .andExpect(jsonPath("$.data.username").value("carry-user"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    /**
     * 验证登录请求参数不合法时返回 200 响应码。
     */
    @Test
    @DisplayName("login invalid request returns code 200")
    void login_invalidRequest_returnsCode200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证登录凭证错误时返回 300 响应码。
     */
    @Test
    @DisplayName("login invalid credentials returns code 300")
    void login_invalidCredentials_returnsCode300() throws Exception {
        when(authApplicationService.login(any()))
                .thenThrow(ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"wrong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300))
                .andExpect(jsonPath("$.message").value("username or password is invalid"));
    }

    /**
     * 验证刷新成功时返回新的 token 数据。
     */
    @Test
    @DisplayName("refresh success returns code 100")
    void refresh_success_returnsCode100() throws Exception {
        when(authApplicationService.refresh(any())).thenReturn(tokenResult());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    /**
     * 验证 refresh token 不合法时返回 300 响应码。
     */
    @Test
    @DisplayName("refresh invalid token returns code 300")
    void refresh_invalidToken_returnsCode300() throws Exception {
        when(authApplicationService.refresh(any()))
                .thenThrow(ProblemException.forbidden("invalid_refresh_session", "refresh token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"invalid-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300))
                .andExpect(jsonPath("$.message").value("refresh token is invalid"));
    }

    /**
     * 验证注销成功时返回统一成功响应。
     */
    @Test
    @DisplayName("logout success returns code 100")
    void logout_success_returnsCode100() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100));
    }

    /**
     * 验证当前用户接口能返回已绑定的请求身份。
     */
    @Test
    @DisplayName("me authenticated request returns current user")
    void me_authenticatedRequest_returnsCurrentUser() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1001L))
                .andExpect(jsonPath("$.data.username").value("carry-user"));
    }

    /**
     * 验证未绑定身份访问当前用户接口会返回 300 响应码。
     */
    @Test
    @DisplayName("me anonymous request returns code 300")
    void me_anonymousRequest_returnsCode300() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300))
                .andExpect(jsonPath("$.message").value("authentication is required"));
    }

    private static class BindPrincipalInterceptor implements HandlerInterceptor {

        private final AuthRequestContext authRequestContext;

        private BindPrincipalInterceptor(AuthRequestContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedPrincipal(1001L, "carry-user"));
            return true;
        }
    }

    private AuthTokenResult tokenResult() {
        return new AuthTokenResult(
                1001L,
                "carry-user",
                "access-token",
                Instant.parse("2026-04-20T12:30:00Z"),
                "refresh-token",
                Instant.parse("2026-05-04T12:00:00Z")
        );
    }
}
