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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelQueryController 协议测试。
 * 职责：验证频道列表、详情和发现查询的 HTTP 输入输出契约。
 * 边界：领域查询由替身提供，不验证数据库访问。
 */
@Tag("contract")
class ChannelQueryControllerTests {

    private static final String DISCOVER_CURSOR_SCOPE = "channel_discover";

    private ChannelQueryApi channelQueryDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = authenticatedMockMvc();
    }

    /**
     * 验证频道列表返回当前账户可见的频道摘要。
     */
    @Test
    @DisplayName("list channels returns channel summaries")
    void listChannels_returnsChannelSummaries() throws Exception {
        when(channelQueryDomainApi.listChannels(1001L)).thenReturn(List.of(
                channel(1L, "General", "public"),
                channel(9L, "project-alpha", "private")
        ));

        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0].cid").value("1"))
                .andExpect(jsonPath("$.channels[0].owner_uid").value("1001"))
                .andExpect(jsonPath("$.channels[1].cid").value("9"));
    }

    /**
     * 验证频道详情返回路径 ID 对应的频道摘要。
     */
    @Test
    @DisplayName("get channel by id returns channel summary")
    void getChannelById_returnsChannelSummary() throws Exception {
        when(channelQueryDomainApi.getChannelById(1001L, 9L)).thenReturn(
                channel(9L, "project-alpha", "private")
        );

        mockMvc.perform(get("/api/channels/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.name").value("project-alpha"))
                .andExpect(jsonPath("$.owner_uid").value("1001"));
    }

    /**
     * 验证频道发现返回游标分页结构和公开字段。
     */
    @Test
    @DisplayName("discover channels returns cursor page")
    void discoverChannels_returnsCursorPage() throws Exception {
        when(channelQueryDomainApi.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover")
                        .param("q", "gen")
                        .param("type", "public")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].member_count").value(42))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证频道发现接受既定 scope 编码的不透明游标。
     */
    @Test
    @DisplayName("discover channels accepts opaque cursor")
    void discoverChannels_acceptsOpaqueCursor() throws Exception {
        when(channelQueryDomainApi.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover")
                        .param("cursor", OpaqueCursorCodec.encode(DISCOVER_CURSOR_SCOPE, 88L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"));
    }

    private ChannelResult channel(long channelId, String name, String type) {
        return new ChannelResult(
                channelId,
                channelId,
                name,
                "讨论区",
                "avatars/ch/" + channelId + ".png",
                "1001",
                type,
                "public".equals(type),
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelQueryController(channelQueryDomainApi, authRequestContext))
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

    /** 测试用认证主体绑定拦截器。 */
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
