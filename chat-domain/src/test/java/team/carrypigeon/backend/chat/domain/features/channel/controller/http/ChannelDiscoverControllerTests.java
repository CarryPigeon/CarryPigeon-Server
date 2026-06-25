package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelGovernanceApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelLifecycleApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelQueryApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.application.service.NotificationPreferenceApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class ChannelDiscoverControllerTests {

    private static final String DISCOVER_CURSOR_SCOPE = "channel_discover";

    private ChannelQueryApplicationService channelQueryApplicationService;
    private ChannelLifecycleApplicationService channelLifecycleApplicationService;
    private ChannelGovernanceApplicationService channelGovernanceApplicationService;
    private NotificationPreferenceApplicationService notificationPreferenceApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryApplicationService = mock(ChannelQueryApplicationService.class);
        channelLifecycleApplicationService = mock(ChannelLifecycleApplicationService.class);
        channelGovernanceApplicationService = mock(ChannelGovernanceApplicationService.class);
        notificationPreferenceApplicationService = mock(NotificationPreferenceApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(controller())
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("discover channels returns cursor page")
    void discoverChannels_returnsCursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryApplicationService.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover").param("q", "gen").param("type", "public").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].member_count").value(42))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    @DisplayName("discover channels accepts opaque cursor")
    void discoverChannels_acceptsOpaqueCursor() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryApplicationService.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover")
                        .param("cursor", OpaqueCursorCodec.encode(DISCOVER_CURSOR_SCOPE, 88L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(controller())
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ChannelController controller() {
        return new ChannelController(
                channelQueryApplicationService,
                channelLifecycleApplicationService,
                channelGovernanceApplicationService,
                notificationPreferenceApplicationService,
                authRequestContext
        );
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
