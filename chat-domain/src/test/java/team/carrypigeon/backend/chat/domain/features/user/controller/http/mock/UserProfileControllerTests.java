package team.carrypigeon.backend.chat.domain.features.user.controller.http.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.user.controller.http.UserProfileController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserEmailCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserProfileController mock 协议测试。
 * 职责：验证 v1 用户 HTTP 资源的公开视图与更新行为。
 * 边界：领域 API、验证码和文件传输能力使用替身，不验证真实业务编排链路。
 */
@Tag("mock")
class UserProfileControllerTests {

    private UserProfileApi userProfileDomainApi;
    private FileTransferApi fileTransferDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userProfileDomainApi = mock(UserProfileApi.class);
        fileTransferDomainApi = mock(FileTransferApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileDomainApi, authRequestContext, fileTransferDomainApi))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证当前用户资料协议会使用认证账号查询资料和邮箱。
     */
    @Test
    @DisplayName("me authenticated request returns current user resource")
    void me_authenticatedRequest_returnsCurrentUserResource() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileDomainApi.getCurrentUserProfile(any())).thenReturn(userProfileResult());
        when(userProfileDomainApi.getCurrentUserEmail(1001L)).thenReturn("carry-user@example.com");

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.email").value("carry-user@example.com"))
                .andExpect(jsonPath("$.nickname").value("carry-user"))
                .andExpect(jsonPath("$.avatar").value("avatars/u/1001.png"));
        ArgumentCaptor<GetCurrentUserProfileCommand> commandCaptor = ArgumentCaptor.forClass(GetCurrentUserProfileCommand.class);
        verify(userProfileDomainApi).getCurrentUserProfile(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
        verify(userProfileDomainApi).getCurrentUserEmail(1001L);
    }

    /**
     * 验证按账户 ID 查询协议会把路径 ID 映射到领域查询命令。
     */
    @Test
    @DisplayName("get by uid authenticated request returns public profile")
    void getByAccountId_authenticatedRequest_returnsPublicProfile() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(userProfileResult());

        mockMvc.perform(get("/api/users/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.nickname").value("carry-user"))
                .andExpect(jsonPath("$.avatar").value("avatars/u/1001.png"));
        ArgumentCaptor<GetUserProfileByAccountIdCommand> commandCaptor =
                ArgumentCaptor.forClass(GetUserProfileByAccountIdCommand.class);
        verify(userProfileDomainApi).getUserProfileByAccountId(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
    }

    /**
     * 验证批量用户资料协议会解析逗号分隔 ID 并查询公开资料。
     */
    @Test
    @DisplayName("list users by ids returns items envelope")
    void listUsers_byIds_returnsItemsEnvelope() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileDomainApi.getPublicUserProfiles(any())).thenReturn(java.util.List.of(userProfileResult()));

        mockMvc.perform(get("/api/users").param("ids", "1001,1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].nickname").value("carry-user"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<Long>> idsCaptor = ArgumentCaptor.forClass((Class) java.util.List.class);
        verify(userProfileDomainApi).getPublicUserProfiles(idsCaptor.capture());
        assertEquals(java.util.List.of(1001L, 1002L), idsCaptor.getValue());
    }

    /**
     * 验证批量查询协议会拒绝非数字 ID 列表。
     */
    @Test
    @DisplayName("list users invalid ids returns 422")
    void listUsers_invalidIds_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(get("/api/users").param("ids", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证批量查询协议会拒绝包含空片段的 ID 列表，避免静默丢弃客户端参数错误。
     */
    @Test
    @DisplayName("list users blank segment ids returns 422")
    void listUsers_blankSegmentIds_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(get("/api/users").param("ids", "1001,,1002"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证邮箱更新协议会把新邮箱和验证码交给领域 API。
     */
    @Test
    @DisplayName("put current user email success returns 204")
    void updateCurrentUserEmail_success_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(userProfileDomainApi).updateCurrentUserEmail(any());

        mockMvc.perform(put("/api/users/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","code":"123456"}
                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UpdateCurrentUserEmailCommand> commandCaptor = ArgumentCaptor.forClass(UpdateCurrentUserEmailCommand.class);
        verify(userProfileDomainApi).updateCurrentUserEmail(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
        assertEquals("new@example.com", commandCaptor.getValue().email());
        assertEquals("123456", commandCaptor.getValue().code());
    }

    /**
     * 验证邮箱更新协议会拒绝超过持久化容量的邮箱。
     */
    @Test
    @DisplayName("put current user email too long email returns 422")
    void updateCurrentUserEmail_tooLongEmail_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(put("/api/users/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"123456"}
                                """.formatted(tooLongEmail())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证资料更新协议会把当前账号和 v1 请求字段映射到领域命令。
     */
    @Test
    @DisplayName("patch current user profile success returns 204")
    void patchCurrentUserProfile_success_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileDomainApi.updateCurrentUserProfile(any())).thenReturn(userProfileResult());

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","avatar":"avatars/u/1001.png","brief":"hello world","sex":0,"birthday":0}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UpdateCurrentUserProfileCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateCurrentUserProfileCommand.class);
        verify(userProfileDomainApi).updateCurrentUserProfile(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
        assertEquals("carry-user", commandCaptor.getValue().nickname());
        assertEquals("avatars/u/1001.png", commandCaptor.getValue().avatarUrl());
        assertEquals("hello world", commandCaptor.getValue().bio());
        assertEquals(0L, commandCaptor.getValue().sex());
        assertEquals(0L, commandCaptor.getValue().birthday());
    }

    /**
     * 验证批量查询协议会拒绝缺失 ID 列表的请求。
     */
    @Test
    @DisplayName("list users missing ids returns 422")
    void listUsers_missingIds_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证背景图上传协议会写入当前账号固定 share key 并返回下载地址。
     */
    @Test
    @DisplayName("upload current user background returns background url")
    void uploadCurrentUserBackground_returnsBackgroundUrl() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(fileTransferDomainApi.uploadProfileBackground(eq(1001L), any(), anyLong(), any()))
                .thenReturn("profile_bg_1001");

        mockMvc.perform(multipart("/api/users/me/background")
                        .file(new MockMultipartFile("background", "bg.png", "image/png", "img".getBytes()))
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.background_url").value("/api/files/download/profile_bg_1001"));
        verify(fileTransferDomainApi).uploadProfileBackground(eq(1001L), eq("image/png"), eq(3L), any());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileDomainApi, authRequestContext, fileTransferDomainApi))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserProfileResult userProfileResult() {
        return new UserProfileResult(
                1001L,
                "carry-user",
                "avatars/u/1001.png",
                "hello world",
                0L,
                0L,
                Instant.parse("2026-04-20T12:00:00Z"),
                Instant.parse("2026-04-21T12:00:00Z")
        );
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private String tooLongEmail() {
        return "a".repeat(309) + "@example.com";
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
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user@example.com"));
            return true;
        }
    }
}
