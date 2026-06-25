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
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.NotificationPreferencesResult;
import team.carrypigeon.backend.chat.domain.features.server.application.service.NotificationPreferenceApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class NotificationPreferenceControllerTests {

    private NotificationPreferenceApplicationService notificationPreferenceApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationPreferenceApplicationService = mock(NotificationPreferenceApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationPreferenceController(notificationPreferenceApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("get notification preferences returns payload")
    void getNotificationPreferences_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(notificationPreferenceApplicationService.getNotificationPreferences(1001L)).thenReturn(new NotificationPreferencesResult(
                new NotificationPreferencesResult.ServerPreferenceResult("all", 0L),
                List.of(new NotificationPreferencesResult.ChannelPreferenceResult("9", "inherit", 0L))
        ));

        mockMvc.perform(get("/api/notification_preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.mode").value("all"))
                .andExpect(jsonPath("$.channels[0].cid").value("9"));
    }

    @Test
    @DisplayName("update server notification preference returns 204")
    void updateServerNotificationPreference_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(notificationPreferenceApplicationService).updateServerPreference(any());

        mockMvc.perform(put("/api/notification_preferences/server")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"mode\":\"muted\"," +
                                "\"muted_until\":0" +
                                "}"))
                .andExpect(status().isNoContent());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new NotificationPreferenceController(notificationPreferenceApplicationService, authRequestContext))
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

    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final RequestAuthenticationContext authRequestContext;
        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) { this.authRequestContext = authRequestContext; }
        @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
