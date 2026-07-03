package team.carrypigeon.backend.chat.domain.features.auth.controller.http.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController mock 协议测试。
 * 职责：验证 v1 邮箱验证码、token 创建、刷新与撤销入口的协议契约。
 * 边界：领域 API 与服务入口 API 使用 Mockito 替身，不验证真实业务编排链路。
 */
@Tag("mock")
class AuthControllerTests {

    private AuthAccountApi authAccountApi;
    private AuthSessionApi authSessionApi;
    private ServerEntranceApi serverEntranceApi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authAccountApi = mock(AuthAccountApi.class);
        authSessionApi = mock(AuthSessionApi.class);
        serverEntranceApi = mock(ServerEntranceApi.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        authAccountApi,
                        authSessionApi,
                        serverEntranceApi,
                        new AuthTokenSettings(Duration.ofMinutes(30), Duration.ofDays(14))
                ))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证邮箱验证码协议会把请求邮箱映射到签发命令。
     */
    @Test
    @DisplayName("send email code success returns 204")
    void sendEmailCode_success_returns204() throws Exception {
        doNothing().when(authAccountApi).sendEmailCode(any());

        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<SendEmailCodeCommand> commandCaptor = ArgumentCaptor.forClass(SendEmailCodeCommand.class);
        verify(authAccountApi).sendEmailCode(commandCaptor.capture());
        assertEquals("user@example.com", commandCaptor.getValue().email());
    }

    /**
     * 验证邮箱验证码协议会拒绝空邮箱并返回参数校验失败。
     */
    @Test
    @DisplayName("send email code invalid request returns 422")
    void sendEmailCode_invalidRequest_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":""}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证邮箱服务不可用时会转换为 503 响应。
     */
    @Test
    @DisplayName("send email code mail service unavailable returns 503")
    void sendEmailCode_mailServiceUnavailable_returns503() throws Exception {
        doThrow(ProblemException.fail("mail_service_unavailable", "mail service is unavailable"))
                .when(authAccountApi)
                .sendEmailCode(any());

        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.reason").value("mail_service_unavailable"))
                .andExpect(jsonPath("$.error.message").value("mail service is unavailable"));
    }

    /**
     * 验证邮箱投递失败时会转换为 503 响应并保留错误原因。
     */
    @Test
    @DisplayName("send email code delivery failure returns 503")
    void sendEmailCode_deliveryFailure_returns503() throws Exception {
        doThrow(ProblemException.fail("email_delivery_failed", "failed to deliver verification email"))
                .when(authAccountApi)
                .sendEmailCode(any());

        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.reason").value("email_delivery_failed"))
                .andExpect(jsonPath("$.error.message").value("failed to deliver verification email"));
    }

    /**
     * 验证会话创建协议会先检查客户端插件，再把授权类型、邮箱和验证码传给领域 API。
     */
    @Test
    @DisplayName("create token session success returns token response")
    void createTokenSession_success_returnsTokenResponse() throws Exception {
        when(serverEntranceApi.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of());
        when(authSessionApi.createTokenSession(any())).thenReturn(tokenResult(true));

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"user@example.com",
                                  "code":"123456",
                                  "client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind","version":"1.2.0"}]}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.is_new_user").value(true));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<String>> pluginsCaptor = ArgumentCaptor.forClass((Class) java.util.List.class);
        verify(serverEntranceApi).findMissingRequiredPlugins(pluginsCaptor.capture());
        assertEquals(java.util.List.of("mc-bind"), pluginsCaptor.getValue());
        ArgumentCaptor<CreateTokenSessionCommand> commandCaptor = ArgumentCaptor.forClass(CreateTokenSessionCommand.class);
        verify(authSessionApi).createTokenSession(commandCaptor.capture());
        assertEquals("email_code", commandCaptor.getValue().grantType());
        assertEquals("user@example.com", commandCaptor.getValue().email());
        assertEquals("123456", commandCaptor.getValue().code());
    }

    /**
     * 验证缺少必需客户端插件时会阻止创建会话并返回缺失插件列表。
     */
    @Test
    @DisplayName("create token session required plugin missing returns 412")
    void createTokenSession_requiredPluginMissing_returns412() throws Exception {
        when(serverEntranceApi.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of("mc-bind"));

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"user@example.com",
                                  "code":"123456",
                                  "client":{"device_id":"device-1","installed_plugins":[]}
                                }
                                """))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error.reason").value("required_plugin_missing"))
                .andExpect(jsonPath("$.error.details.missing_plugins[0]").value("mc-bind"));
    }

    /**
     * 验证会话创建协议会拒绝缺失授权要素的请求。
     */
    @Test
    @DisplayName("create token session invalid request returns 422")
    void createTokenSession_invalidRequest_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grant_type":"","email":"bad","code":"","client":null}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证邮箱验证码无效时会返回参数校验失败响应。
     */
    @Test
    @DisplayName("create token session invalid code returns 422")
    void createTokenSession_invalidCode_returns422() throws Exception {
        when(serverEntranceApi.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of());
        when(authSessionApi.createTokenSession(any())).thenThrow(ProblemException.validationFailed("email code is invalid"));

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"user@example.com",
                                  "code":"000000",
                                  "client":{"device_id":"device-1","installed_plugins":[]}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证注册协议会规范化用户名空白并传递明文密码输入。
     */
    @Test
    @DisplayName("register success returns 201 and uid")
    void register_success_returns201AndUid() throws Exception {
        when(authAccountApi.register(any())).thenReturn(new RegisterResult(1001L, "carry-user"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.username").value("carry-user"));
        ArgumentCaptor<RegisterCommand> commandCaptor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(authAccountApi).register(commandCaptor.capture());
        assertEquals("carry-user", commandCaptor.getValue().username());
        assertEquals("password123", commandCaptor.getValue().password());
    }

    /**
     * 验证登录协议会规范化用户名空白并返回标准 Bearer token 响应。
     */
    @Test
    @DisplayName("login success returns token response")
    void login_success_returnsTokenResponse() throws Exception {
        when(authSessionApi.login(any())).thenReturn(new AuthTokenResult(
                1001L,
                "carry-user",
                "access-token",
                Instant.parse("2026-06-02T13:30:00Z"),
                "refresh-token",
                Instant.parse("2026-06-16T13:00:00Z")
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.is_new_user").value(false))
                .andExpect(jsonPath("$.expires_in").value(1800));
        ArgumentCaptor<LoginCommand> commandCaptor = ArgumentCaptor.forClass(LoginCommand.class);
        verify(authSessionApi).login(commandCaptor.capture());
        assertEquals("carry-user", commandCaptor.getValue().username());
        assertEquals("password123", commandCaptor.getValue().password());
    }

    /**
     * 验证刷新协议会把 refresh token 映射为刷新命令。
     */
    @Test
    @DisplayName("refresh success returns token response")
    void refresh_success_returnsTokenResponse() throws Exception {
        when(authSessionApi.refreshTokenSession(any())).thenReturn(tokenResult(false));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"refresh-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.is_new_user").value(false));
        ArgumentCaptor<RefreshTokenCommand> commandCaptor = ArgumentCaptor.forClass(RefreshTokenCommand.class);
        verify(authSessionApi).refreshTokenSession(commandCaptor.capture());
        assertEquals("refresh-token", commandCaptor.getValue().refreshToken());
    }

    /**
     * 验证刷新协议会拒绝空 refresh token 和缺失客户端信息。
     */
    @Test
    @DisplayName("refresh invalid request returns 422")
    void refresh_invalidRequest_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"","client":null}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证无效 refresh token 会被转换为未认证响应。
     */
    @Test
    @DisplayName("refresh invalid token returns 401")
    void refresh_invalidToken_returns401() throws Exception {
        when(authSessionApi.refreshTokenSession(any()))
                .thenThrow(ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"refresh_token":"bad-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));
    }

    /**
     * 验证撤销协议会把 refresh token 映射为注销命令。
     */
    @Test
    @DisplayName("revoke success returns 204")
    void revoke_success_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"refresh-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<LogoutCommand> commandCaptor = ArgumentCaptor.forClass(LogoutCommand.class);
        verify(authSessionApi).logout(commandCaptor.capture());
        assertEquals("refresh-token", commandCaptor.getValue().refreshToken());
    }

    /**
     * 验证撤销无效 refresh token 时会返回未认证响应。
     */
    @Test
    @DisplayName("revoke invalid token returns 401")
    void revoke_invalidToken_returns401() throws Exception {
        doThrow(ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"))
                .when(authSessionApi)
                .logout(any());

        mockMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"refresh_token":"bad-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));
    }

    private AuthSessionTokenResult tokenResult(boolean newUser) {
        return new AuthSessionTokenResult(1001L, "access-token", 1800L, "refresh-token", newUser);
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
