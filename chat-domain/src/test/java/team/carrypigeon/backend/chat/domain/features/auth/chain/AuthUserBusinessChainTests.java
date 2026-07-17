package team.carrypigeon.backend.chat.domain.features.auth.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthAccessTokenInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthAccountDomainApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthPasswordLoginPolicy;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthSessionDomainApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.features.user.controller.http.UserProfileController;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserProfileDomainApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证到用户资料的真实业务链路测试。
 * 职责：验证 HTTP 协议入口、真实领域 API、认证拦截器和内存仓储替身之间的最小闭环。
 * 边界：不连接 MySQL、Redis、MinIO 或邮件服务；外部端口使用确定性测试替身。
 */
@Tag("business")
class AuthUserBusinessChainTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-20T12:00:00Z");

    /**
     * 验证邮箱验证码会话创建后可以用真实 access token 访问并更新当前用户资料。
     */
    @Test
    @DisplayName("email token session creates account and authenticated profile api reflects updates")
    void emailTokenSession_createsAccountAndAuthenticatedProfileApi_reflectsUpdates() throws Exception {
        Fixture fixture = new Fixture();

        MvcResult tokenResult = fixture.authMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"User@Example.com",
                                  "code":"123456",
                                  "client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind","version":"1.0.0"}]}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.is_new_user").value(true))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();
        JsonNode tokenResponse = fixture.readJson(tokenResult);
        String accessToken = tokenResponse.get("access_token").asText();

        fixture.userMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("user"));

        fixture.userMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","avatar":"avatars/u/1001.png","brief":"hello world","sex":0,"birthday":0}
                                """))
                .andExpect(status().isNoContent());

        fixture.userMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("carry-user"))
                .andExpect(jsonPath("$.avatar").value("avatars/u/1001.png"));
        assertTrue(fixture.channelMemberRepository.exists(1L, 1001L));
        assertTrue(fixture.channelMemberRepository.exists(2L, 1001L));
    }

    /**
     * 验证邮箱验证码会话链路会拒绝缺失 required plugin 的客户端。
     */
    @Test
    @DisplayName("email token session missing required plugin returns precondition failed")
    void emailTokenSession_missingRequiredPlugin_returnsPreconditionFailed() throws Exception {
        Fixture fixture = new Fixture();
        fixture.serverEntranceApi.missingPlugins = List.of("mc-bind");

        fixture.authMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"user@example.com",
                                  "code":"123456",
                                  "client":{"device_id":"device-1","installed_plugins":[]}
                                }
                                """))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error.reason").value("required_plugin_missing"))
                .andExpect(jsonPath("$.error.details.missing_plugins[0]").value("mc-bind"));
        assertFalse(fixture.accountRepository.findByUsername("user@example.com").isPresent());
    }

    /**
     * 验证邮箱验证码错误时不会创建账户或用户资料。
     */
    @Test
    @DisplayName("email token session invalid code returns validation error")
    void emailTokenSession_invalidCode_returnsValidationError() throws Exception {
        Fixture fixture = new Fixture();

        fixture.authMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"email_code",
                                  "email":"user@example.com",
                                  "code":"000000",
                                  "client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind"}]}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("email code is invalid"));
        assertFalse(fixture.accountRepository.findByUsername("user@example.com").isPresent());
    }

    /**
     * 验证不支持的授权类型会被真实领域 API 拒绝。
     */
    @Test
    @DisplayName("email token session unsupported grant type returns validation error")
    void emailTokenSession_unsupportedGrantType_returnsValidationError() throws Exception {
        Fixture fixture = new Fixture();

        fixture.authMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type":"password",
                                  "email":"user@example.com",
                                  "code":"123456",
                                  "client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind"}]}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("grant_type must be email_code"));
    }

    /**
     * 验证登录、刷新和撤销会贯穿 HTTP 入口与真实 refresh session 仓储状态。
     */
    @Test
    @DisplayName("login refresh and revoke rotate and revoke refresh sessions")
    void loginRefreshAndRevoke_validCredentials_rotateAndRevokeRefreshSessions() throws Exception {
        Fixture fixture = new Fixture();
        fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("1001"));

        MvcResult loginResult = fixture.authMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andReturn();
        String firstRefreshToken = fixture.readJson(loginResult).get("refresh_token").asText();
        long firstSessionId = fixture.tokenService.refreshSessionId(firstRefreshToken);

        MvcResult refreshResult = fixture.authMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String secondRefreshToken = fixture.readJson(refreshResult).get("refresh_token").asText();
        long secondSessionId = fixture.tokenService.refreshSessionId(secondRefreshToken);

        assertTrue(fixture.refreshSessionRepository.findById(firstSessionId).orElseThrow().revoked());
        assertTrue(fixture.refreshSessionRepository.findById(secondSessionId).isPresent());

        fixture.authMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(secondRefreshToken)))
                .andExpect(status().isNoContent());

        assertTrue(fixture.refreshSessionRepository.findById(secondSessionId).orElseThrow().revoked());
    }

    /**
     * 验证重复注册会返回参数校验错误且不会覆盖既有账户。
     */
    @Test
    @DisplayName("register duplicate username returns validation error")
    void register_duplicateUsername_returnsValidationError() throws Exception {
        Fixture fixture = new Fixture();
        fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("username already exists"));
    }

    /**
     * 验证账号不存在或密码错误时登录链路返回权限失败语义。
     */
    @Test
    @DisplayName("login invalid credentials returns forbidden")
    void login_invalidCredentials_returnsForbidden() throws Exception {
        Fixture fixture = new Fixture();
        fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        fixture.authMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"wrong-password"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("forbidden"))
                .andExpect(jsonPath("$.error.message").value("username or password is invalid"));

        fixture.authMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing-user","password":"password123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("forbidden"))
                .andExpect(jsonPath("$.error.message").value("username or password is invalid"));
    }

    /**
     * 验证无效 refresh token 会被刷新链路拒绝。
     */
    @Test
    @DisplayName("refresh invalid token returns unauthorized")
    void refresh_invalidToken_returnsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();

        fixture.authMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"bad-token","client":{"device_id":"device-1"}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"))
                .andExpect(jsonPath("$.error.message").value("refresh token is invalid"));
    }

    /**
     * 验证已撤销 refresh session 不能再次刷新。
     */
    @Test
    @DisplayName("refresh revoked session returns unauthorized")
    void refresh_revokedSession_returnsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();
        MvcResult loginResult = fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals("1001", fixture.readJson(loginResult).get("uid").asText());
        MvcResult tokenResult = fixture.authMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = fixture.readJson(tokenResult).get("refresh_token").asText();
        fixture.authMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        fixture.authMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));
    }

    /**
     * 验证 refresh 轮换后，旧 refresh token 不能再通过撤销接口被当作有效会话处理。
     */
    @Test
    @DisplayName("revoke rotated refresh token returns unauthorized")
    void revoke_rotatedRefreshToken_returnsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();
        fixture.authMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isCreated());
        MvcResult tokenResult = fixture.authMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carry-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String firstRefreshToken = fixture.readJson(tokenResult).get("refresh_token").asText();
        long firstSessionId = fixture.tokenService.refreshSessionId(firstRefreshToken);
        MvcResult refreshResult = fixture.authMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String secondRefreshToken = fixture.readJson(refreshResult).get("refresh_token").asText();
        long secondSessionId = fixture.tokenService.refreshSessionId(secondRefreshToken);

        fixture.authMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"%s","client":{"device_id":"device-1"}}
                                """.formatted(firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));

        assertTrue(fixture.refreshSessionRepository.findById(firstSessionId).orElseThrow().revoked());
        assertFalse(fixture.refreshSessionRepository.findById(secondSessionId).orElseThrow().revoked());
    }

    /**
     * 验证受保护用户资料接口会拒绝缺失或非法 access token。
     */
    @Test
    @DisplayName("user profile protected endpoints invalid access token returns unauthorized")
    void userProfileProtectedEndpoints_invalidAccessToken_returnsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();

        fixture.userMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"));

        fixture.userMvc.perform(get("/api/users/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"))
                .andExpect(jsonPath("$.error.message").value("access token is invalid"));
    }

    /**
     * 验证按账号读取和批量读取会贯穿认证、controller 和真实用户资料领域 API。
     */
    @Test
    @DisplayName("user profile read endpoints authenticated token return public profiles")
    void userProfileReadEndpoints_authenticatedToken_returnPublicProfiles() throws Exception {
        Fixture fixture = new Fixture();
        String accessToken = fixture.createEmailSessionAccessToken("user@example.com");

        fixture.userMvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.nickname").value("user"));

        fixture.userMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("ids", "1001,9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].nickname").value("user"));
    }

    /**
     * 验证缺失用户资料时受保护接口返回 404 问题语义。
     */
    @Test
    @DisplayName("current user profile missing profile returns not found")
    void currentUserProfile_missingProfile_returnsNotFound() throws Exception {
        Fixture fixture = new Fixture();
        String accessToken = fixture.tokenService.issueAccessToken(
                new AuthAccount(1001L, "missing@example.com", "", BASE_TIME, BASE_TIME),
                BASE_TIME.plus(Duration.ofMinutes(30))
        );

        fixture.userMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"))
                .andExpect(jsonPath("$.error.message").value("user profile does not exist"));
    }

    /**
     * 验证邮箱更新链路会校验验证码、归一化邮箱并反映在当前用户资料响应中。
     */
    @Test
    @DisplayName("update current user email valid code updates normalized email")
    void updateCurrentUserEmail_validCode_updatesNormalizedEmail() throws Exception {
        Fixture fixture = new Fixture();
        String accessToken = fixture.createEmailSessionAccessToken("user@example.com");

        fixture.userMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"New@Example.com","code":"123456"}
                                """))
                .andExpect(status().isNoContent());

        fixture.userMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    /**
     * 验证邮箱更新会拒绝错误验证码和重复邮箱。
     */
    @Test
    @DisplayName("update current user email invalid code or duplicate email returns validation error")
    void updateCurrentUserEmail_invalidCodeOrDuplicateEmail_returnsValidationError() throws Exception {
        Fixture fixture = new Fixture();
        String accessToken = fixture.createEmailSessionAccessToken("user@example.com");
        fixture.createEmailSessionAccessToken("other@example.com");

        fixture.userMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","code":"000000"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("email code is invalid"));

        fixture.userMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"other@example.com","code":"123456"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("email already exists"));
    }

    /**
     * 验证背景图上传链路会经过认证并调用文件传输端口。
     */
    @Test
    @DisplayName("upload current user background authenticated token stores fixed share key")
    void uploadCurrentUserBackground_authenticatedToken_storesFixedShareKey() throws Exception {
        Fixture fixture = new Fixture();
        String accessToken = fixture.createEmailSessionAccessToken("user@example.com");

        fixture.userMvc.perform(multipart("/api/users/me/background")
                        .file(new MockMultipartFile("background", "bg.png", "image/png", "img".getBytes()))
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.background_url").value("/api/files/download/profile_bg_1001"));

        assertEquals(1001L, fixture.fileTransferApi.lastAccountId);
        assertEquals("image/png", fixture.fileTransferApi.lastContentType);
    }

    /**
     * `Fixture` 测试夹具。
     * 职责：装配真实 controller、领域 API 和确定性内存替身，形成不依赖外部服务的业务链路。
     */
    private static final class Fixture {

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        private final InMemoryAuthAccountRepository accountRepository = new InMemoryAuthAccountRepository();
        private final InMemoryAuthRefreshSessionRepository refreshSessionRepository = new InMemoryAuthRefreshSessionRepository();
        private final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        private final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        private final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        private final DeterministicIdGenerator idGenerator = new DeterministicIdGenerator();
        private final DeterministicAuthTokenService tokenService = new DeterministicAuthTokenService();
        private final NoopServerEntranceApi serverEntranceApi = new NoopServerEntranceApi();
        private final RecordingFileTransferApi fileTransferApi = new RecordingFileTransferApi();
        private final AuthTokenSettings tokenSettings = new AuthTokenSettings(Duration.ofMinutes(30), Duration.ofDays(14));
        private final MockMvc authMvc;
        private final MockMvc userMvc;

        private Fixture() {
            TimeProvider timeProvider = new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC));
            TransactionRunner transactionRunner = new NoopTransactionRunner();
            PasswordHasher passwordHasher = new PrefixPasswordHasher();
            TokenHasher tokenHasher = token -> "hash::" + token;
            EmailVerificationCodeService emailVerificationCodeService = new AcceptingEmailVerificationCodeService();
            AuthAccountDomainApi accountApi = new AuthAccountDomainApi(
                    accountRepository,
                    userProfileRepository,
                    channelRepository,
                    channelMemberRepository,
                    passwordHasher,
                    idGenerator,
                    timeProvider,
                    transactionRunner,
                    emailVerificationCodeService
            );
            AuthSessionDomainApi sessionApi = new AuthSessionDomainApi(
                    accountRepository,
                    refreshSessionRepository,
                    userProfileRepository,
                    channelRepository,
                    channelMemberRepository,
                    passwordHasher,
                    tokenHasher,
                    tokenService,
                    tokenSettings,
                    new AuthPasswordLoginPolicy(true),
                    idGenerator,
                    timeProvider,
                    transactionRunner,
                    emailVerificationCodeService
            );
            RequestAuthenticationContext authRequestContext = new RequestAuthenticationContext();
            UserProfileDomainApi userProfileApi = new UserProfileDomainApi(
                    accountRepository,
                    userProfileRepository,
                    emailVerificationCodeService,
                    timeProvider,
                    transactionRunner
            );
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
            this.authMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                            accountApi,
                            sessionApi,
                            serverEntranceApi
                    ))
                    .setMessageConverters(converter)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
            this.userMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(
                            userProfileApi,
                            authRequestContext,
                            fileTransferApi
                    ))
                    .addInterceptors(new AuthAccessTokenInterceptor(tokenService, authRequestContext))
                    .setMessageConverters(converter)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        private JsonNode readJson(MvcResult result) throws Exception {
            return objectMapper.readTree(result.getResponse().getContentAsString());
        }

        private String createEmailSessionAccessToken(String email) throws Exception {
            MvcResult tokenResult = authMvc.perform(post("/api/auth/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "grant_type":"email_code",
                                      "email":"%s",
                                      "code":"123456",
                                      "client":{"device_id":"device-1","installed_plugins":[{"plugin_id":"mc-bind"}]}
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isOk())
                    .andReturn();
            return readJson(tokenResult).get("access_token").asText();
        }
    }

    /**
     * `InMemoryAuthAccountRepository` 内存仓储。
     * 职责：记录账户读写结果，使链路测试能观察真实领域副作用。
     */
    private static final class InMemoryAuthAccountRepository implements AuthAccountRepository {

        private final Map<Long, AuthAccount> accountsById = new HashMap<>();
        private final Map<String, AuthAccount> accountsByUsername = new HashMap<>();

        @Override
        public Optional<AuthAccount> findByUsername(String username) {
            return Optional.ofNullable(accountsByUsername.get(username));
        }

        @Override
        public Optional<AuthAccount> findById(long accountId) {
            return Optional.ofNullable(accountsById.get(accountId));
        }

        @Override
        public AuthAccount save(AuthAccount account) {
            accountsById.put(account.id(), account);
            accountsByUsername.put(account.username(), account);
            return account;
        }

        @Override
        public AuthAccount update(AuthAccount account) {
            accountsById.put(account.id(), account);
            accountsByUsername.values().removeIf(existing -> existing.id() == account.id());
            accountsByUsername.put(account.username(), account);
            return account;
        }
    }

    /**
     * `InMemoryAuthRefreshSessionRepository` 内存仓储。
     * 职责：记录 refresh session 的保存和撤销状态。
     */
    private static final class InMemoryAuthRefreshSessionRepository implements AuthRefreshSessionRepository {

        private final Map<Long, AuthRefreshSession> sessions = new HashMap<>();

        @Override
        public Optional<AuthRefreshSession> findById(long sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public AuthRefreshSession save(AuthRefreshSession session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public void revoke(long sessionId) {
            AuthRefreshSession session = sessions.get(sessionId);
            sessions.put(sessionId, new AuthRefreshSession(
                    session.id(),
                    session.accountId(),
                    session.refreshTokenHash(),
                    session.expiresAt(),
                    true,
                    session.createdAt(),
                    BASE_TIME
            ));
        }
    }

    /**
     * `InMemoryUserProfileRepository` 内存仓储。
     * 职责：保存账号初始化和用户资料更新产生的资料状态。
     */
    private static final class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profilesByAccountId = new HashMap<>();

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profilesByAccountId.get(accountId));
        }

        @Override
        public List<UserProfile> findAll() {
            return new ArrayList<>(profilesByAccountId.values());
        }

        @Override
        public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return profilesByAccountId.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            return profilesByAccountId.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .filter(profile -> profile.nickname().contains(keyword) || profile.bio().contains(keyword))
                    .limit(limit)
                    .toList();
        }

        @Override
        public UserProfile save(UserProfile userProfile) {
            profilesByAccountId.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

        @Override
        public UserProfile update(UserProfile userProfile) {
            profilesByAccountId.put(userProfile.accountId(), userProfile);
            return userProfile;
        }
    }

    /**
     * `InMemoryChannelRepository` 内存仓储。
     * 职责：提供账号初始化需要的默认频道和 system 频道。
     */
    private static final class InMemoryChannelRepository implements ChannelRepository {

        private final Map<Long, Channel> channels = new HashMap<>();

        private InMemoryChannelRepository() {
            channels.put(1L, new Channel(1L, 1L, "public", "", "", "", "public", true, BASE_TIME, BASE_TIME));
            channels.put(2L, new Channel(2L, 2L, "system", "", "", "", "system", false, BASE_TIME, BASE_TIME));
        }

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channels.get(1L));
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return channels.values().stream().filter(channel -> "system".equals(channel.type())).findFirst();
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }
    }

    /**
     * `InMemoryChannelMemberRepository` 内存仓储。
     * 职责：记录账号初始化时写入的默认频道成员关系。
     */
    private static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        private final Map<Long, List<Long>> membersByChannelId = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).contains(accountId);
        }

        @Override
        public void save(ChannelMember channelMember) {
            membersByChannelId.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>())
                    .add(channelMember.accountId());
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return membersByChannelId.getOrDefault(channelId, List.of());
        }
    }

    /**
     * `DeterministicAuthTokenService` 确定性令牌服务。
     * 职责：签发可解析的测试 token，使 HTTP 认证拦截器参与链路验证。
     */
    private static final class DeterministicAuthTokenService implements AuthTokenService {

        @Override
        public String issueAccessToken(AuthAccount account, Instant expiresAt) {
            return "access:%d:%s:%d".formatted(account.id(), account.username(), expiresAt.toEpochMilli());
        }

        @Override
        public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
            return "refresh:%d:%s:%d:%d".formatted(account.id(), account.username(), refreshSessionId, expiresAt.toEpochMilli());
        }

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            if (accessToken == null || !accessToken.startsWith("access:")) {
                throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
            }
            String[] parts = accessToken.split(":");
            return new AuthTokenClaims(parts[1], parts[2], "access", 0L, Instant.ofEpochMilli(Long.parseLong(parts[3])));
        }

        @Override
        public AuthTokenClaims parseRefreshToken(String refreshToken) {
            if (refreshToken == null || !refreshToken.startsWith("refresh:")) {
                throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
            }
            String[] parts = refreshToken.split(":");
            return new AuthTokenClaims(parts[1], parts[2], "refresh", Long.parseLong(parts[3]), Instant.ofEpochMilli(Long.parseLong(parts[4])));
        }

        private long refreshSessionId(String refreshToken) {
            return Long.parseLong(refreshToken.split(":")[3]);
        }
    }

    /**
     * `DeterministicIdGenerator` 确定性 ID 生成器。
     * 职责：让链路测试中的账户和 refresh session ID 稳定可断言。
     */
    private static final class DeterministicIdGenerator implements IdGenerator {

        private long next = 1001L;

        @Override
        public long nextLongId() {
            return next++;
        }
    }

    /**
     * `PrefixPasswordHasher` 密码哈希替身。
     * 职责：提供确定性密码摘要，避免链路测试依赖具体密码学实现。
     */
    private static final class PrefixPasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed::" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return hash(rawPassword).equals(passwordHash);
        }
    }

    /**
     * `AcceptingEmailVerificationCodeService` 验证码替身。
     * 职责：让邮箱验证码链路可通过，同时仍经过真实领域 API 的验证码校验调用点。
     */
    private static final class AcceptingEmailVerificationCodeService implements EmailVerificationCodeService {

        @Override
        public void issueCode(String email) {
        }

        @Override
        public void verifyCode(String email, String code) {
            if (!"123456".equals(code)) {
                throw ProblemException.validationFailed("email code is invalid");
            }
        }
    }

    /**
     * `NoopTransactionRunner` 事务替身。
     * 职责：保留领域 API 的事务边界调用点，不引入真实数据库事务。
     */
    private static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    /**
     * `NoopServerEntranceApi` 服务入口替身。
     * 职责：让鉴权 HTTP 链路通过 required plugin gate，不验证服务发现领域。
     */
    private static final class NoopServerEntranceApi implements ServerEntranceApi {

        private List<String> missingPlugins = List.of();

        @Override
        public ServerDiscoveryDocument getServerDiscoveryDocument() {
            throw new UnsupportedOperationException("server discovery is not part of this business chain");
        }

        @Override
        public List<String> findMissingRequiredPlugins(List<String> installedPluginIds) {
            return missingPlugins;
        }

        @Override
        public void requireRequiredPluginsSatisfied(List<String> installedPluginIds) {
            if (!missingPlugins.isEmpty()) {
                throw ProblemException.validationFailed(
                        "required_plugin_missing",
                        "required plugins are missing",
                        Map.of("missing_plugins", missingPlugins)
                );
            }
        }
    }

    /**
     * `RecordingFileTransferApi` 文件传输替身。
     * 职责：记录背景图上传入参，使用户资料背景链路可验证。
     */
    private static final class RecordingFileTransferApi implements FileTransferApi {

        private long lastAccountId;
        private String lastShareKey;
        private String lastContentType;
        private long lastSizeBytes;

        @Override
        public FileUploadGrantResult createUploadGrant(long accountId, String filename, String mimeType, long sizeBytes) {
            throw new UnsupportedOperationException("file upload grant is not part of this business chain");
        }

        @Override
        public void uploadFile(long accountId, String shareKey, String contentType, long sizeBytes, InputStream content) {
            this.lastAccountId = accountId;
            this.lastShareKey = shareKey;
            this.lastContentType = contentType;
            this.lastSizeBytes = sizeBytes;
        }

        @Override
        public String uploadProfileBackground(long accountId, String contentType, long sizeBytes, InputStream content) {
            this.lastAccountId = accountId;
            this.lastShareKey = "profile_bg_" + accountId;
            this.lastContentType = contentType;
            this.lastSizeBytes = sizeBytes;
            return lastShareKey;
        }

        @Override
        public Optional<FileDownloadResult> downloadFile(Long accountId, String shareKey) {
            return Optional.empty();
        }

        @Override
        public boolean isServerAvatar(String shareKey) {
            return false;
        }

        @Override
        public Map<String, String> uploadHeaders() {
            return Map.of();
        }
    }
}
