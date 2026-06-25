package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

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
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationFlowApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class ChannelApplicationControllerTests {

    private ChannelApplicationFlowApplicationService channelApplicationFlowApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelApplicationFlowApplicationService = mock(ChannelApplicationFlowApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelApplicationController(channelApplicationFlowApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("create channel application returns payload")
    void createChannelApplication_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationFlowApplicationService.createChannelApplication(any()))
                .thenReturn(new ChannelApplicationResult(3001L, 9L, 1002L, "hi", Instant.parse("2026-04-24T12:00:00Z"), "PENDING"));

        mockMvc.perform(post("/api/channels/9/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"reason\":\"hi\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application_id").value("3001"))
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    @DisplayName("list channel applications returns items")
    void listChannelApplications_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationFlowApplicationService.listChannelApplications(any()))
                .thenReturn(List.of(new ChannelApplicationResult(3001L, 9L, 1002L, "hi", Instant.parse("2026-04-24T12:00:00Z"), "PENDING")));

        mockMvc.perform(get("/api/channels/9/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].application_id").value("3001"))
                .andExpect(jsonPath("$.items[0].uid").value("1002"));
    }

    @Test
    @DisplayName("decide channel application returns payload")
    void decideChannelApplication_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationFlowApplicationService.decideChannelApplication(any()))
                .thenReturn(new ChannelApplicationResult(3001L, 9L, 1002L, "", Instant.parse("2026-04-24T12:00:00Z"), "ACCEPTED"));

        mockMvc.perform(post("/api/channels/9/applications/3001/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"decision\":\"approve\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application_id").value("3001"))
                .andExpect(jsonPath("$.status").value("accepted"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelApplicationController(channelApplicationFlowApplicationService, authRequestContext))
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

        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
