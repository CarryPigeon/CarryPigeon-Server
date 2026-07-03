package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainApiTestSupport.account;
import static team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainApiTestSupport.createAccountApi;
import static team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainApiTestSupport.newFixture;

/**
 * AuthAccountDomainApi 注册契约测试。
 * 职责：验证注册流程的成功、校验失败和事务回滚语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务编排。
 */
@Tag("contract")
class AuthAccountDomainApiRegistrationTests {

    /**
     * 验证注册成功时会返回账户结果、保存哈希后的密码并初始化默认资料。
     */
    @Test
    @DisplayName("register new account returns result stores hashed password and provisions default profile")
    void register_newAccount_returnsResultStoresHashedPasswordAndProvisionsDefaultProfile() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();

        RegisterResult result = fixture.accountApi.register(new RegisterCommand("carry-user", "password123"));

        assertEquals(1001L, result.accountId());
        assertEquals("carry-user", result.username());
        assertTrue(fixture.accountRepository.findByUsername("carry-user").isPresent());
        assertNotEquals("password123", fixture.accountRepository.findByUsername("carry-user").orElseThrow().passwordHash());
        UserProfile userProfile = fixture.userProfileRepository.findByAccountId(1001L).orElseThrow();
        assertEquals("carry-user", userProfile.nickname());
        assertEquals("", userProfile.avatarUrl());
        assertEquals("", userProfile.bio());
        assertTrue(fixture.channelMemberRepository.exists(1L, 1001L));
        assertTrue(fixture.channelMemberRepository.exists(2L, 1001L));
        assertTrue(fixture.channelRepository.findSystemChannel().isPresent());
    }

    /**
     * 验证用户名重复时会中断注册并返回参数类问题语义。
     */
    @Test
    @DisplayName("register duplicate username throws validation problem")
    void register_duplicateUsername_throwsValidationProblem() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();
        fixture.accountRepository.save(account(1L, "carry-user", "hashed::existing"));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.accountApi.register(new RegisterCommand("carry-user", "password123"))
        );

        assertEquals("username already exists", exception.getMessage());
    }

    /**
     * 验证默认资料初始化失败时注册事务会整体回滚，避免留下不完整账户。
     */
    @Test
    @DisplayName("register profile provisioning failure rolls back account creation")
    void register_profileProvisioningFailure_rollsBackAccountCreation() {
        AuthDomainApiTestSupport.InMemoryAuthAccountRepository accountRepository =
                new AuthDomainApiTestSupport.InMemoryAuthAccountRepository();
        AuthDomainApiTestSupport.InMemoryAuthRefreshSessionRepository refreshSessionRepository =
                new AuthDomainApiTestSupport.InMemoryAuthRefreshSessionRepository();
        AuthDomainApiTestSupport.InMemoryUserProfileRepository userProfileRepository =
                new AuthDomainApiTestSupport.InMemoryUserProfileRepository();
        AuthDomainApiTestSupport.InMemoryChannelRepository channelRepository =
                new AuthDomainApiTestSupport.InMemoryChannelRepository();
        AuthDomainApiTestSupport.InMemoryChannelMemberRepository channelMemberRepository =
                new AuthDomainApiTestSupport.InMemoryChannelMemberRepository();
        userProfileRepository.failOnSave = true;
        AuthAccountDomainApi accountApi = createAccountApi(
                accountRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                new AuthDomainApiTestSupport.SnapshotTransactionRunner(
                        accountRepository,
                        userProfileRepository,
                        channelMemberRepository
                )
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> accountApi.register(new RegisterCommand("carry-user", "password123"))
        );

        assertEquals("profile provisioning failed", exception.getMessage());
        assertFalse(accountRepository.findByUsername("carry-user").isPresent());
        assertFalse(userProfileRepository.findByAccountId(1001L).isPresent());
        assertFalse(channelMemberRepository.exists(1L, 1001L));
    }

    /**
     * 验证缺少 system 频道时注册会中断，而不是在流程中隐式创建新频道。
     */
    @Test
    @DisplayName("register missing system channel throws internal problem")
    void register_missingSystemChannel_throwsInternalProblem() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();
        fixture.channelRepository.channels.entrySet().removeIf(entry -> "system".equals(entry.getValue().type()));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.accountApi.register(new RegisterCommand("carry-user", "password123"))
        );

        assertEquals("system channel does not exist", exception.getMessage());
        assertEquals(0, fixture.channelRepository.savedChannels.size());
    }
}
