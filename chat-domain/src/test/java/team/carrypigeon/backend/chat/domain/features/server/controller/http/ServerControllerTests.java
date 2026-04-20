package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ServerController 协议测试。
 * 职责：验证 HTTP 基础入口的统一响应码与异常映射契约。
 * 边界：不验证 Netty 通道生命周期，只验证 Spring MVC 入口层。
 */
@WebMvcTest(ServerController.class)
@Import({ServerApplicationService.class, GlobalExceptionHandler.class})
class ServerControllerTests {

    @SpringBootConfiguration
    @ComponentScan(basePackages = "team.carrypigeon.backend.chat.domain")
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证基础概览接口成功路径返回统一成功响应。
     */
    @Test
    @DisplayName("summary success returns code 100")
    void summary_success_returnsCode100() throws Exception {
        mockMvc.perform(get("/api/server/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.service").value("carry-pigeon-backend"));
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

}
