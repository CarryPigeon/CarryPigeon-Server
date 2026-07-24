package team.carrypigeon.backend.chat.domain.features.plugin.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.plugin.config.PluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.PluginCatalogDomainApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.MessageTypePluginRegistrationSupport;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 插件目录 HTTP 协议测试。
 */
@Tag("contract")
class PluginCatalogControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChannelMessagePluginRegistry registry = new PluginConfiguration().channelMessagePluginRegistry(java.util.List.of(
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
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PluginCatalogController(new PluginCatalogDomainApi(registry, java.util.List.of("mc-bind"))))
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
