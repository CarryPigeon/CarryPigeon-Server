package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ServerController 协议测试。
 * 职责：验证 HTTP 基础入口的统一响应码与异常映射契约。
 * 边界：不验证 Netty 通道生命周期，只验证协议层请求到响应的稳定行为。
 */
@Tag("contract")
class ServerControllerTests {

    private MockMvc mockMvc;
    private ServerApplicationService serverApplicationService;
    private AuthRequestContext authRequestContext;
    private RealtimeSessionRegistry realtimeSessionRegistry;

    @BeforeEach
    void setUp() {
        authRequestContext = new AuthRequestContext();
        realtimeSessionRegistry = new RealtimeSessionRegistry();
        serverApplicationService = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(
                        registration("builtin-voice-message", "voice", "voice", true),
                        registration("builtin-file-message", "file", "file", true),
                        registration("builtin-custom-message", "custom", "custom", true),
                        registration("builtin-plugin-message", "plugin", "plugin", true),
                        registration("builtin-text-message", "text", "text", true)
                )),
                realtimeProperties(true),
                realtimeSessionRegistry
        );
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ServerController(serverApplicationService, authRequestContext),
                        new ServerWellKnownController(serverApplicationService)
                )
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证参数校验失败时返回 200 响应码。
     */
    @Test
    @DisplayName("echo blank content returns code 200")
    void echo_blankContent_returnsCode200() throws Exception {
        mockMvc.perform(post("/api/server/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证公开源信息接口可匿名访问并返回最小公开字段。
     */
    @Test
    @DisplayName("well known server document anonymous request returns code 100")
    void wellKnownServerDocument_anonymousRequest_returnsCode100() throws Exception {
        mockMvc.perform(get("/.well-known/carrypigeon-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.server_id").value("carrypigeon-local"))
                .andExpect(jsonPath("$.data.server_name").value("CarryPigeonBackend"))
                .andExpect(jsonPath("$.data.register_enabled").value(true))
                .andExpect(jsonPath("$.data.login_methods[0]").value("username_password"))
                .andExpect(jsonPath("$.data.public_capabilities[0]").value("user_registration"))
                .andExpect(jsonPath("$.data.public_capabilities[1]").value("username_password_login"))
                .andExpect(jsonPath("$.data.public_plugins[0]").value("custom"))
                .andExpect(jsonPath("$.data.public_plugins[1]").value("file"))
                .andExpect(jsonPath("$.data.public_plugins[2]").value("plugin"))
                .andExpect(jsonPath("$.data.public_plugins[3]").value("text"))
                .andExpect(jsonPath("$.data.public_plugins[4]").value("voice"));
    }

    /**
     * 验证已移除的匿名 summary 路径不会继续作为公开源契约存在。
     */
    @Test
    @DisplayName("summary endpoint removed returns 404")
    void summary_removedEndpoint_returns404() throws Exception {
        mockMvc.perform(get("/api/server/summary"))
                .andExpect(status().isNotFound());
    }

    /**
     * 验证 presence 接口在 realtime 启用且存在会话时返回 ONLINE。
     */
    @Test
    @DisplayName("current presence authenticated online request returns online")
    void currentPresence_authenticatedOnlineRequest_returnsOnline() throws Exception {
        realtimeSessionRegistry.register(1001L, new EmbeddedChannel());

        mockMvc.perform(get("/api/server/presence/me").with(authenticated(1001L, "carry-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.account_id").value(1001))
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.online_session_count").value(1));
    }

    /**
     * 验证 presence 接口在缺少认证主体时返回鉴权失败语义。
     */
    @Test
    @DisplayName("current presence anonymous request returns code 300")
    void currentPresence_anonymousRequest_returnsCode300() throws Exception {
        mockMvc.perform(get("/api/server/presence/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    private ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            boolean publicVisible
    ) {
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        publicPluginKey,
                        "test plugin",
                        publicVisible,
                        java.util.List.of("message.sent"),
                        java.util.List.of("message:" + messageType + ":send"),
                        "test_condition"
                ),
                new TextChannelMessagePlugin() {
                    @Override
                    public String supportedType() {
                        return messageType;
                    }
                }
        );
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private RealtimeServerProperties realtimeProperties(boolean enabled) {
        return new RealtimeServerProperties(enabled, "127.0.0.1", 28080, "/ws", 1, 0);
    }

    private RequestPostProcessor authenticated(long accountId, String username) {
        return request -> {
            authRequestContext.bind((HttpServletRequest) request, new AuthenticatedPrincipal(accountId, username));
            return request;
        };
    }
}
