package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelMessageController 附件协议测试。
 * 职责：验证频道消息附件上传入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库访问与查询链路，只验证上传协议层行为。
 */
@Tag("contract")
class ChannelMessageAttachmentControllerTests {

    private MessageApplicationService messageApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageApplicationService = mock(MessageApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证已认证成员可以上传频道消息附件。
     */
    @Test
    @DisplayName("upload channel message attachment authenticated request returns code 100")
    void uploadChannelMessageAttachment_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.uploadChannelMessageAttachment(any())).thenReturn(
                new ChannelMessageAttachmentUploadResult(
                        "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "demo.pdf",
                        "application/pdf",
                        123L
                )
        );

        mockMvc.perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo".getBytes()))
                        .param("messageType", "file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.objectKey").value("channels/1/messages/file/accounts/1001/5001-demo.pdf"))
                .andExpect(jsonPath("$.data.filename").value("demo.pdf"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.data.size").value(123L));
    }

    /**
     * 验证附件上传参数错误时会返回 200 响应码。
     */
    @Test
    @DisplayName("upload channel message attachment invalid type returns code 200")
    void uploadChannelMessageAttachment_invalidType_returnsCode200() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.uploadChannelMessageAttachment(any()))
                .thenThrow(ProblemException.validationFailed("messageType must be file or voice"));

        mockMvc.perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo".getBytes()))
                        .param("messageType", "image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证非成员上传附件时会返回 300 响应码。
     */
    @Test
    @DisplayName("upload channel message attachment non member returns code 300")
    void uploadChannelMessageAttachment_nonMember_returnsCode300() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.uploadChannelMessageAttachment(any()))
                .thenThrow(ProblemException.forbidden("channel_member_required", "channel membership is required"));

        mockMvc.perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo".getBytes()))
                        .param("messageType", "file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    /**
     * 验证附件上传未预期异常会被映射为 500 响应码。
     */
    @Test
    @DisplayName("upload channel message attachment unexpected failure returns code 500")
    void uploadChannelMessageAttachment_unexpectedFailure_returnsCode500() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.uploadChannelMessageAttachment(any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo".getBytes()))
                        .param("messageType", "file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static class BindPrincipalInterceptor implements HandlerInterceptor {

        private final AuthRequestContext authRequestContext;

        private BindPrincipalInterceptor(AuthRequestContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedPrincipal(1001L, "carry-user"));
            return true;
        }
    }
}
