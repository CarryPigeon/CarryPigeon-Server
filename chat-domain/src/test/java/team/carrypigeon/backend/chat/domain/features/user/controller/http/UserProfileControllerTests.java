package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.file.application.service.FileApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserProfileController 协议测试。
 * 职责：验证 v1 用户 HTTP 资源的公开视图与更新行为。
 * 边界：不验证真实数据库访问与验证码服务实现，只验证协议层输出。
 */
@Tag("contract")
class UserProfileControllerTests {

    private UserProfileApplicationService userProfileApplicationService;
    private EmailVerificationCodeService emailVerificationCodeService;
    private FileApplicationService fileApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userProfileApplicationService = mock(UserProfileApplicationService.class);
        emailVerificationCodeService = mock(EmailVerificationCodeService.class);
        fileApplicationService = mock(FileApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileApplicationService, emailVerificationCodeService, authRequestContext, fileApplicationService))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("me authenticated request returns current user resource")
    void me_authenticatedRequest_returnsCurrentUserResource() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getCurrentUserProfile(any())).thenReturn(userProfileResult());
        when(userProfileApplicationService.getCurrentUserEmail(1001L)).thenReturn("carry-user@example.com");

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.email").value("carry-user@example.com"))
                .andExpect(jsonPath("$.nickname").value("carry-user"))
                .andExpect(jsonPath("$.avatar").value("avatars/u/1001.png"));
    }

    @Test
    @DisplayName("get by uid authenticated request returns public profile")
    void getByAccountId_authenticatedRequest_returnsPublicProfile() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(userProfileResult());

        mockMvc.perform(get("/api/users/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.nickname").value("carry-user"))
                .andExpect(jsonPath("$.avatar").value("avatars/u/1001.png"));
    }

    @Test
    @DisplayName("list users by ids returns items envelope")
    void listUsers_byIds_returnsItemsEnvelope() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getPublicUserProfiles(any())).thenReturn(java.util.List.of(userProfileResult()));

        mockMvc.perform(get("/api/users").param("ids", "1001,1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].nickname").value("carry-user"));
    }

    @Test
    @DisplayName("list users invalid ids returns 422")
    void listUsers_invalidIds_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(get("/api/users").param("ids", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    @Test
    @DisplayName("put current user email success returns 204")
    void updateCurrentUserEmail_success_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(emailVerificationCodeService).verifyCode(any(), any());

        mockMvc.perform(put("/api/users/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","code":"123456"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("patch current user profile success returns 204")
    void patchCurrentUserProfile_success_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.updateCurrentUserProfile(any())).thenReturn(userProfileResult());

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","avatar":"avatars/u/1001.png","brief":"hello world","sex":0,"birthday":0}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("list users missing ids returns 422")
    void listUsers_missingIds_returns422() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    @Test
    @DisplayName("upload current user background returns background url")
    void uploadCurrentUserBackground_returnsBackgroundUrl() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(fileApplicationService).uploadFile(eq(1001L), eq("profile_bg_1001"), any(), anyLong(), any());

        mockMvc.perform(multipart("/api/users/me/background")
                        .file(new MockMultipartFile("background", "bg.png", "image/png", "img".getBytes()))
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.background_url").value("api/files/download/profile_bg_1001"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileApplicationService, emailVerificationCodeService, authRequestContext, fileApplicationService))
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
                Instant.parse("2026-04-20T12:00:00Z"),
                Instant.parse("2026-04-21T12:00:00Z")
        );
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

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
