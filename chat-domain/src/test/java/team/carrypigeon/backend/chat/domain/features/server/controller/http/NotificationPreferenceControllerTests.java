package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.NotificationPreferencesResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `NotificationPreferenceController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class NotificationPreferenceControllerTests {

    private NotificationPreferenceApi notificationPreferenceDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationPreferenceDomainApi = mock(NotificationPreferenceApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationPreferenceController(notificationPreferenceDomainApi, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `getNotificationPreferences` 在 `returnsPayload` 场景下的测试契约。
     */
    @Test
    @DisplayName("get notification preferences returns payload")
    void getNotificationPreferences_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(notificationPreferenceDomainApi.getNotificationPreferences(1001L)).thenReturn(new NotificationPreferencesResult(
                new NotificationPreferencesResult.ServerPreferenceResult("all", 0L),
                List.of(new NotificationPreferencesResult.ChannelPreferenceResult("9", "inherit", 0L))
        ));

        mockMvc.perform(get("/api/notification_preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.mode").value("all"))
                .andExpect(jsonPath("$.channels[0].cid").value("9"));
    }

    /**
     * 验证 `updateServerNotificationPreference` 在 `returns204` 场景下的测试契约。
     */
    @Test
    @DisplayName("update server notification preference returns 204")
    void updateServerNotificationPreference_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(notificationPreferenceDomainApi).updateServerPreference(any());

        mockMvc.perform(put("/api/notification_preferences/server")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"mode\":\"muted\"," +
                                "\"muted_until\":0" +
                                "}"))
                .andExpect(status().isNoContent());
    }

    /**
     * 验证服务级通知偏好请求体为 JSON null 时返回稳定校验错误。
     */
    @Test
    @DisplayName("update server notification preference null body returns validation error")
    void updateServerNotificationPreference_nullBody_returnsValidationError() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(put("/api/notification_preferences/server")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new NotificationPreferenceController(notificationPreferenceDomainApi, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    /**
     * `BindPrincipalInterceptor` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final RequestAuthenticationContext authRequestContext;
        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) { this.authRequestContext = authRequestContext; }
        @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
