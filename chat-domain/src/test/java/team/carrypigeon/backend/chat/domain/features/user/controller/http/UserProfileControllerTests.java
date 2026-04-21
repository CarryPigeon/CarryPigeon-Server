package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserProfileController 协议测试。
 * 职责：验证当前用户资料查询与更新入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库访问，只验证协议层请求到响应的稳定行为。
 */
class UserProfileControllerTests {

    private UserProfileApplicationService userProfileApplicationService;

    private AuthRequestContext authRequestContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userProfileApplicationService = mock(UserProfileApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证已认证请求可以读取当前用户资料。
     */
    @Test
    @DisplayName("me authenticated request returns current user profile")
    void me_authenticatedRequest_returnsCurrentUserProfile() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getCurrentUserProfile(any())).thenReturn(userProfileResult());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1001L))
                .andExpect(jsonPath("$.data.nickname").value("carry-user"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://img.example/avatar.png"))
                .andExpect(jsonPath("$.data.bio").value("hello world"));
    }

    /**
     * 验证未认证请求访问资料查询接口时会返回 300 响应码。
     */
    @Test
    @DisplayName("me anonymous request returns code 300")
    void me_anonymousRequest_returnsCode300() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300))
                .andExpect(jsonPath("$.message").value("authentication is required"));
    }

    /**
     * 验证资料不存在时查询接口会返回 404 响应码。
     */
    @Test
    @DisplayName("me missing profile returns code 404")
    void me_missingProfile_returnsCode404() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getCurrentUserProfile(any()))
                .thenThrow(ProblemException.notFound("user profile does not exist"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("user profile does not exist"));
    }

    /**
     * 验证未预期异常会被查询接口映射为 500 响应码。
     */
    @Test
    @DisplayName("me unexpected failure returns code 500")
    void me_unexpectedFailure_returnsCode500() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.getCurrentUserProfile(any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("internal server error"));
    }

    /**
     * 验证已认证请求可以更新当前用户资料。
     */
    @Test
    @DisplayName("update me authenticated request returns updated profile")
    void updateMe_authenticatedRequest_returnsUpdatedProfile() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.updateCurrentUserProfile(any())).thenReturn(userProfileResult());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user","avatarUrl":"https://img.example/avatar.png","bio":"hello world"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1001L))
                .andExpect(jsonPath("$.data.nickname").value("carry-user"));
    }

    /**
     * 验证更新请求允许头像地址和简介为空字符串。
     */
    @Test
    @DisplayName("update me blank avatar url and bio returns code 100")
    void updateMe_blankAvatarUrlAndBio_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.updateCurrentUserProfile(any())).thenReturn(new UserProfileResult(
                1001L,
                "carry-user",
                "",
                "",
                Instant.parse("2026-04-20T12:00:00Z"),
                Instant.parse("2026-04-21T12:00:00Z")
        ));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user","avatarUrl":"","bio":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.avatarUrl").value(""))
                .andExpect(jsonPath("$.data.bio").value(""));
    }

    /**
     * 验证更新请求参数不合法时会返回 200 响应码。
     */
    @Test
    @DisplayName("update me invalid request returns code 200")
    void updateMe_invalidRequest_returnsCode200() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"","avatarUrl":"https://img.example/avatar.png","bio":"hello world"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证更新请求缺少可清空字段时仍会返回 200 响应码，避免 null 下沉到数据库层。
     */
    @Test
    @DisplayName("update me missing nullable storage fields returns code 200")
    void updateMe_missingNullableStorageFields_returnsCode200() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证未认证请求访问资料更新接口时会返回 300 响应码。
     */
    @Test
    @DisplayName("update me anonymous request returns code 300")
    void updateMe_anonymousRequest_returnsCode300() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user","avatarUrl":"https://img.example/avatar.png","bio":"hello world"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300))
                .andExpect(jsonPath("$.message").value("authentication is required"));
    }

    /**
     * 验证资料不存在时更新接口会返回 404 响应码。
     */
    @Test
    @DisplayName("update me missing profile returns code 404")
    void updateMe_missingProfile_returnsCode404() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.updateCurrentUserProfile(any()))
                .thenThrow(ProblemException.notFound("user profile does not exist"));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user","avatarUrl":"https://img.example/avatar.png","bio":"hello world"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("user profile does not exist"));
    }

    /**
     * 验证未预期异常会被更新接口映射为 500 响应码。
     */
    @Test
    @DisplayName("update me unexpected failure returns code 500")
    void updateMe_unexpectedFailure_returnsCode500() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(userProfileApplicationService.updateCurrentUserProfile(any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"carry-user","avatarUrl":"https://img.example/avatar.png","bio":"hello world"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("internal server error"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserProfileResult userProfileResult() {
        return new UserProfileResult(
                1001L,
                "carry-user",
                "https://img.example/avatar.png",
                "hello world",
                Instant.parse("2026-04-20T12:00:00Z"),
                Instant.parse("2026-04-21T12:00:00Z")
        );
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
