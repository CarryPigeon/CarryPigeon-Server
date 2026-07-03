package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Domain 目录 HTTP 协议测试。
 */
@Tag("contract")
class ServerDomainCatalogControllerTests {

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
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ServerDomainCatalogController(new MessagePluginCatalogDomainApi(registry)))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `getDomainCatalog` 在 `returnsPublicDomains` 场景下的测试契约。
     */
    @Test
    @DisplayName("get domain catalog returns public domains")
    void getDomainCatalog_returnsPublicDomains() throws Exception {
        mockMvc.perform(get("/api/domains/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].domain").value("Core:Text"))
                .andExpect(jsonPath("$.items[0].supported_versions[0]").value("1.0.0"))
                .andExpect(jsonPath("$.items[0].providers[0].type").value("core"));
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
