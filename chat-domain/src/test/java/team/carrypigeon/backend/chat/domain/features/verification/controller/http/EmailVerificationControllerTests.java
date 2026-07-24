package team.carrypigeon.backend.chat.domain.features.verification.controller.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EmailVerificationController 契约测试。
 * 职责：验证既有邮箱验证码 HTTP 路由在 verification feature 中保持请求映射和校验语义。
 * 边界：领域签发逻辑由 API mock 隔离，不连接缓存或邮件服务。
 */
@Tag("mock")
class EmailVerificationControllerTests {

    private EmailVerificationApi emailVerificationApi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        emailVerificationApi = mock(EmailVerificationApi.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EmailVerificationController(emailVerificationApi))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证合法请求仍返回 204，并把邮箱映射到 verification 签发命令。
     */
    @Test
    @DisplayName("send email code valid request returns 204")
    void sendEmailCode_validRequest_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<IssueEmailVerificationCodeCommand> captor =
                ArgumentCaptor.forClass(IssueEmailVerificationCodeCommand.class);
        verify(emailVerificationApi).issueCode(captor.capture());
        assertEquals("user@example.com", captor.getValue().email());
    }

    /**
     * 验证非法邮箱仍由统一协议边界映射为 422 validation_failed。
     */
    @Test
    @DisplayName("send email code invalid email returns 422")
    void sendEmailCode_invalidEmail_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/email_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }
}
