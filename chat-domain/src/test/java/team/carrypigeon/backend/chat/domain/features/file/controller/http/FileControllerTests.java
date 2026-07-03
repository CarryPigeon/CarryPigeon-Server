package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `FileController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class FileControllerTests {

    private FileTransferApi fileTransferDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fileTransferDomainApi = mock(FileTransferApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileTransferDomainApi, authRequestContext))
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new ResourceHttpMessageConverter(), snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `createUpload` 在 `authenticatedRequest` 条件下满足 `returnsUploadGrant` 的测试契约。
     */
    @Test
    @DisplayName("create upload authenticated request returns upload grant")
    void createUpload_authenticatedRequest_returnsUploadGrant() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(fileTransferDomainApi.createUploadGrant(anyLong(), any(), any(), anyLong()))
                .thenReturn(new FileUploadGrantResult(7001L, "shr_7001", "/api/files/uploads/shr_7001", Instant.parse("2026-04-23T01:00:00Z")));
        when(fileTransferDomainApi.uploadHeaders()).thenReturn(java.util.Map.of());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/files/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"filename\":\"image.png\"," +
                                "\"mime_type\":\"image/png\"," +
                                "\"size_bytes\":123" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file_id").value("7001"))
                .andExpect(jsonPath("$.share_key").value("shr_7001"))
                .andExpect(jsonPath("$.upload.method").value("PUT"))
                .andExpect(jsonPath("$.upload.url").value("/api/files/uploads/shr_7001"));
    }

    /**
     * 验证 `download` 在 `serverAvatar` 条件下满足 `anonymousRequest_returnsFileBody` 的测试契约。
     */
    @Test
    @DisplayName("download server avatar anonymous request returns file body")
    void download_serverAvatar_anonymousRequest_returnsFileBody() throws Exception {
        when(fileTransferDomainApi.isServerAvatar("server_avatar")).thenReturn(true);
        when(fileTransferDomainApi.downloadFile(null, "server_avatar"))
                .thenReturn(Optional.of(new FileDownloadResult(
                        "image/png",
                        10L,
                        Optional.of(new java.io.ByteArrayInputStream("avatar-bytes".getBytes())),
                        Optional.empty()
                )));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files/download/server_avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    /**
     * 验证 `download` 在 `privateFile` 条件下满足 `authenticatedRequest_returnsFileBody` 的测试契约。
     */
    @Test
    @DisplayName("download private file authenticated request returns file body")
    void download_privateFile_authenticatedRequest_returnsFileBody() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(fileTransferDomainApi.isServerAvatar("shr_7001")).thenReturn(false);
        when(fileTransferDomainApi.downloadFile(1001L, "shr_7001"))
                .thenReturn(Optional.of(new FileDownloadResult(
                        "application/pdf",
                        12L,
                        Optional.of(new java.io.ByteArrayInputStream("demo-content".getBytes())),
                        Optional.empty()
                )));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files/download/shr_7001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    /**
     * 验证 `uploadFile` 在 `authenticatedRequest` 条件下满足 `returns204` 的测试契约。
     */
    @Test
    @DisplayName("upload file authenticated request returns 204")
    void uploadFile_authenticatedRequest_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(fileTransferDomainApi).uploadFile(eq(1001L), eq("shr_7001"), eq("application/pdf"), eq(12L), any());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/files/uploads/shr_7001")
                        .contentType(MediaType.APPLICATION_PDF)
                        .content("demo-content"))
                .andExpect(status().isNoContent());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new FileController(fileTransferDomainApi, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new ResourceHttpMessageConverter(), snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    /**
     * `BindPrincipalInterceptor` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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
