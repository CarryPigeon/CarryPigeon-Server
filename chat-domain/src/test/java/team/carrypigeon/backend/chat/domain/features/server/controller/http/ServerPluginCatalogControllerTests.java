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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessagePluginCatalogDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.MessageTypePluginRegistrationSupport;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.ServerEntranceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 插件目录 HTTP 协议测试。
 */
@Tag("contract")
class ServerPluginCatalogControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChannelMessagePluginRegistry registry = new MessagePluginConfiguration().channelMessagePluginRegistry(java.util.List.of(
                MessageTypePluginRegistrationSupport.registration(
                        "builtin-text-message",
                        "text",
                        "text",
                        "Built-in text channel message plugin",
                        true,
                        java.util.List.of("message:text:send"),
                        "always_available",
                        new TextChannelMessagePlugin()
                )
        ));
        ServerEntranceApi service = new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                new RealtimeDiscoverySettings(true, "127.0.0.1", 28080, "/api/ws"),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("mc-bind")
        );
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ServerPluginCatalogController(new MessagePluginCatalogDomainApi(registry), service))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `getPluginCatalog` 在 `returnsPublicPlugins` 场景下的测试契约。
     */
    @Test
    @DisplayName("get plugin catalog returns public plugins")
    void getPluginCatalog_returnsPublicPlugins() throws Exception {
        mockMvc.perform(get("/api/plugins/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required_plugins[0]").value("mc-bind"))
                .andExpect(jsonPath("$.plugins[0].plugin_id").value("text"))
                .andExpect(jsonPath("$.plugins[0].name").value("Built-in text channel message plugin"))
                .andExpect(jsonPath("$.plugins[0].provides_domains[0].domain").value("Core:Text"))
                .andExpect(jsonPath("$.plugins[0].provides_domains[0].domain_version").value("1.0.0"));
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
