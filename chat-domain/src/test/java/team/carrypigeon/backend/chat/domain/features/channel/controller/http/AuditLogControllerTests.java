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
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `AuditLogController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class AuditLogControllerTests {

    private static final String AUDIT_CURSOR_SCOPE = "audit_logs";

    private ChannelQueryApi channelQueryDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(channelQueryDomainApi, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `listAuditLogs` 在 `returnsCursorPage` 场景下的测试契约。
     */
    @Test
    @DisplayName("list audit logs returns cursor page")
    void listAuditLogs_returnsCursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listAuditLogs(any())).thenReturn(List.of(
                new AuditLogResult("7001", "9", "1001", "channel.ban.create", "{}", 1714305600000L)
        ));

        mockMvc.perform(get("/api/audit_logs").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].audit_id").value("7001"))
                .andExpect(jsonPath("$.items[0].action").value("channel.ban.create"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证 `listAuditLogs` 在 `acceptsOpaqueCursor` 场景下的测试契约。
     */
    @Test
    @DisplayName("list audit logs accepts opaque cursor")
    void listAuditLogs_acceptsOpaqueCursor() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listAuditLogs(any())).thenReturn(List.of(
                new AuditLogResult("7001", "9", "1001", "channel.ban.create", "{}", 1714305600000L)
        ));

        mockMvc.perform(get("/api/audit_logs")
                        .param("cursor", OpaqueCursorCodec.encode(AUDIT_CURSOR_SCOPE, 7000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].audit_id").value("7001"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new AuditLogController(channelQueryDomainApi, authRequestContext))
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
