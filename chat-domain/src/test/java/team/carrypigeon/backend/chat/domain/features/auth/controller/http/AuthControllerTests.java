package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 协议测试。
 * 职责：验证 v1 邮箱验证码、token 创建、刷新与撤销入口的协议契约。
 * 边界：不验证真实数据库、验证码发送或 JWT 解析实现，只验证协议层行为。
 */
@Tag("contract")
class AuthControllerTests {

    private AuthApplicationService authApplicationService;
    private ServerApplicationService serverApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authApplicationService = mock(AuthApplicationService.class);
        serverApplicationService = mock(ServerApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        authApplicationService,
                        serverApplicationService,
                        new AuthJwtProperties("carrypigeon", "0123456789abcdef0123456789abcdef", Duration.ofMinutes(30), Duration.ofDays(14))
                ))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("send email code success returns 204")
    void sendEmailCode_success_returns204() throws Exception {
        doNothing().when(authApplicationService).sendEmailCode(any());

        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isNoContent());
    }

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

    @Test
    @DisplayName("send email code mail service unavailable returns 503")
    void sendEmailCode_mailServiceUnavailable_returns503() throws Exception {
        doThrow(ProblemException.fail("mail_service_unavailable", "mail service is unavailable"))
                .when(authApplicationService)
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

    @Test
    @DisplayName("send email code delivery failure returns 503")
    void sendEmailCode_deliveryFailure_returns503() throws Exception {
        doThrow(ProblemException.fail("email_delivery_failed", "failed to deliver verification email"))
                .when(authApplicationService)
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

    @Test
    @DisplayName("create token session success returns token response")
    void createTokenSession_success_returnsTokenResponse() throws Exception {
        when(serverApplicationService.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of());
        when(authApplicationService.createTokenSession(any())).thenReturn(tokenResult(true));

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
    }

    @Test
    @DisplayName("create token session required plugin missing returns 412")
    void createTokenSession_requiredPluginMissing_returns412() throws Exception {
        when(serverApplicationService.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of("mc-bind"));

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

    @Test
    @DisplayName("create token session invalid code returns 422")
    void createTokenSession_invalidCode_returns422() throws Exception {
        when(serverApplicationService.findMissingRequiredPlugins(any())).thenReturn(java.util.List.of());
        when(authApplicationService.createTokenSession(any())).thenThrow(ProblemException.validationFailed("email code is invalid"));

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

    @Test
    @DisplayName("register success returns 201 and uid")
    void register_success_returns201AndUid() throws Exception {
        when(authApplicationService.register(any())).thenReturn(new RegisterResult(1001L, "carry-user"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.username").value("carry-user"));
    }

    @Test
    @DisplayName("login success returns token response")
    void login_success_returnsTokenResponse() throws Exception {
        when(authApplicationService.login(any())).thenReturn(new AuthTokenResult(
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
    }

    @Test
    @DisplayName("refresh success returns token response")
    void refresh_success_returnsTokenResponse() throws Exception {
        when(authApplicationService.refreshTokenSession(any())).thenReturn(tokenResult(false));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"refresh-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.is_new_user").value(false));
    }

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

    @Test
    @DisplayName("refresh invalid token returns 401")
    void refresh_invalidToken_returns401() throws Exception {
        when(authApplicationService.refreshTokenSession(any()))
                .thenThrow(ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"refresh_token":"bad-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));
    }

    @Test
    @DisplayName("revoke success returns 204")
    void revoke_success_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"refresh-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("revoke invalid token returns 401")
    void revoke_invalidToken_returns401() throws Exception {
        doThrow(ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"))
                .when(authApplicationService)
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
