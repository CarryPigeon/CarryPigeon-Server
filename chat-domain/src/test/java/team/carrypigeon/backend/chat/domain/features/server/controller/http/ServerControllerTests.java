package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.ServerEntranceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ServerController 协议测试。
 * 职责：验证 v1 服务发现与 required gate 入口的协议契约。
 * 边界：不验证 Netty 业务链路，只验证 HTTP 层输出。
 */
@Tag("contract")
class ServerControllerTests {

    private MockMvc mockMvc;
    private ServerEntranceApi serverEntranceDomainApi;

    @BeforeEach
    void setUp() {
        serverEntranceDomainApi = createService(true, java.util.List.of("mc-bind"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ServerController(serverEntranceDomainApi),
                        new ServerGateController(serverEntranceDomainApi)
                )
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `serverDiscovery` 在 `anonymousRequest` 条件下满足 `returnsDiscoveryDocument` 的测试契约。
     */
    @Test
    @DisplayName("server discovery anonymous request returns discovery document")
    void serverDiscovery_anonymousRequest_returnsDiscoveryDocument() throws Exception {
        mockMvc.perform(get("/api/server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_id").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.name").value("CarryPigeonBackend"))
                .andExpect(jsonPath("$.brief").value("A self-hosted chat server"))
                .andExpect(jsonPath("$.avatar").value("/api/files/download/server_avatar"))
                .andExpect(jsonPath("$.api_version").value("1.0"))
                .andExpect(jsonPath("$.min_supported_api_version").value("1.0"))
                .andExpect(jsonPath("$.ws_url").value("ws://127.0.0.1:28080/api/ws"))
                .andExpect(jsonPath("$.required_plugins[0]").value("mc-bind"))
                .andExpect(jsonPath("$.capabilities.message_domains").value(true))
                .andExpect(jsonPath("$.capabilities.plugin_catalog").value(true))
                .andExpect(jsonPath("$.capabilities.event_resume").value(true))
                .andExpect(jsonPath("$.server_time").value(1713614400000L));
    }

    /**
     * 验证 `requiredGateCheck` 在 `satisfied` 条件下满足 `returnsEmptyMissingPlugins` 的测试契约。
     */
    @Test
    @DisplayName("required gate check returns empty missing plugins when satisfied")
    void requiredGateCheck_satisfied_returnsEmptyMissingPlugins() throws Exception {
        mockMvc.perform(post("/api/gates/required/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind","version":"1.2.0"}]}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing_plugins").isEmpty());
    }

    /**
     * 验证 `requiredGateCheck` 在 `unsatisfied` 条件下满足 `returnsMissingPlugins` 的测试契约。
     */
    @Test
    @DisplayName("required gate check returns missing plugins when unsatisfied")
    void requiredGateCheck_unsatisfied_returnsMissingPlugins() throws Exception {
        mockMvc.perform(post("/api/gates/required/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client":{"device_id":"device-1","installed_plugins":[]}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing_plugins[0]").value("mc-bind"));
    }

    private ServerEntranceApi createService(boolean realtimeEnabled, java.util.List<String> requiredPlugins) {
        return new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                realtimeProperties(realtimeEnabled),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                requiredPlugins
        );
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private RealtimeDiscoverySettings realtimeProperties(boolean enabled) {
        return new RealtimeDiscoverySettings(enabled, "127.0.0.1", 28080, "/api/ws");
    }
}
