package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.file.application.dto.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.application.service.FileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class FileControllerTests {

    private FileApplicationService fileApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fileApplicationService = mock(FileApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileApplicationService, authRequestContext))
                .setMessageConverters(new ByteArrayHttpMessageConverter(), snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("create upload authenticated request returns upload grant")
    void createUpload_authenticatedRequest_returnsUploadGrant() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(fileApplicationService.createUploadGrant(anyLong(), any(), any(), anyLong()))
                .thenReturn(new FileUploadGrantResult(7001L, "shr_7001", "/api/files/uploads/shr_7001", Instant.parse("2026-04-23T01:00:00Z")));
        when(fileApplicationService.uploadHeaders()).thenReturn(java.util.Map.of());

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

    @Test
    @DisplayName("download server avatar anonymous request returns file body")
    void download_serverAvatar_anonymousRequest_returnsFileBody() throws Exception {
        when(fileApplicationService.isServerAvatar("server_avatar")).thenReturn(true);
        when(fileApplicationService.findStorageObject(null, "server_avatar"))
                .thenReturn(Optional.of(StorageObject.withContent("server_avatar", "image/png", 10L, new java.io.ByteArrayInputStream("avatar-bytes".getBytes()))));
        when(fileApplicationService.createDownloadUrl(null, "server_avatar"))
                .thenReturn(new PresignedUrl(URI.create("http://test.local/objects/server_avatar"), Instant.parse("2026-04-23T01:00:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files/download/server_avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    @DisplayName("download private file authenticated request returns file body")
    void download_privateFile_authenticatedRequest_returnsFileBody() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(fileApplicationService.isServerAvatar("shr_7001")).thenReturn(false);
        when(fileApplicationService.findStorageObject(1001L, "shr_7001"))
                .thenReturn(Optional.of(StorageObject.withContent("files/shr_7001", "application/pdf", 12L, new java.io.ByteArrayInputStream("demo-content".getBytes()))));
        when(fileApplicationService.createDownloadUrl(1001L, "shr_7001"))
                .thenReturn(new PresignedUrl(URI.create("http://test.local/objects/files/shr_7001"), Instant.parse("2026-04-23T01:00:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files/download/shr_7001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @DisplayName("upload file authenticated request returns 204")
    void uploadFile_authenticatedRequest_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(fileApplicationService).uploadFile(eq(1001L), eq("shr_7001"), eq("application/pdf"), eq(12L), any());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/files/uploads/shr_7001")
                        .contentType(MediaType.APPLICATION_PDF)
                        .content("demo-content"))
                .andExpect(status().isNoContent());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new FileController(fileApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(new ByteArrayHttpMessageConverter(), snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
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
