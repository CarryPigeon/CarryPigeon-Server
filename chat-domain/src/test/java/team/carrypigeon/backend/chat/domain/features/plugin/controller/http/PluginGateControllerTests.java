package team.carrypigeon.backend.chat.domain.features.plugin.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.PluginCatalogDomainApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Required plugin gate HTTP 协议测试。
 * 职责：验证插件 feature 独立承载客户端插件准入预检查。
 */
@Tag("contract")
class PluginGateControllerTests {

    @Test
    @DisplayName("required gate reports missing plugins")
    void check_missingPlugin_returnsMissingPlugins() throws Exception {
        PluginCatalogDomainApi pluginCatalogApi = new PluginCatalogDomainApi(
                new ChannelMessagePluginRegistry(java.util.List.of()),
                java.util.List.of("mc-bind")
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PluginGateController(pluginCatalogApi))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/gates/required/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client":{"device_id":"device-1","installed_plugins":[]}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing_plugins[0]").value("mc-bind"));
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
