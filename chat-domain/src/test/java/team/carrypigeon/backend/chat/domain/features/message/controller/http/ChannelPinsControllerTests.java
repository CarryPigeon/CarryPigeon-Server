package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
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
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class ChannelPinsControllerTests {

    private static final String PIN_CURSOR_SCOPE = "channel_pins";

    private MessageApplicationService messageApplicationService;
    private UserProfileApplicationService userProfileApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageApplicationService = mock(MessageApplicationService.class);
        userProfileApplicationService = mock(UserProfileApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, userProfileApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("pin channel message returns pinned item")
    void pinChannelMessage_returnsPinnedItem() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.pinChannelMessage(any()))
                .thenReturn(new ChannelPinResult(7001L, 9L, 5001L, 1001L, Instant.parse("2026-04-24T12:00:00Z"), "重要通知"));

        mockMvc.perform(post("/api/channels/9/pins/5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"note\":\"重要通知\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.mid").value("5001"))
                .andExpect(jsonPath("$.pinned_by_uid").value("1001"));
    }

    @Test
    @DisplayName("unpin channel message returns 204")
    void unpinChannelMessage_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(messageApplicationService).unpinChannelMessage(any());

        mockMvc.perform(delete("/api/channels/9/pins/5001"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("list channel pins returns cursor page")
    void listChannelPins_returnsCursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.listChannelPins(any()))
                .thenReturn(List.of(new ChannelPinResult(7001L, 9L, 5001L, 1001L, Instant.parse("2026-04-24T12:00:00Z"), "重要通知")));

        mockMvc.perform(get("/api/channels/9/pins?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].mid").value("5001"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    @DisplayName("list channel pins accepts opaque cursor")
    void listChannelPins_acceptsOpaqueCursor() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.listChannelPins(any()))
                .thenReturn(List.of(new ChannelPinResult(7001L, 9L, 5001L, 1001L, Instant.parse("2026-04-24T12:00:00Z"), "重要通知")));

        mockMvc.perform(get("/api/channels/9/pins")
                        .param("cursor", OpaqueCursorCodec.encode(PIN_CURSOR_SCOPE, 5000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5001"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, userProfileApplicationService, authRequestContext))
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
}
