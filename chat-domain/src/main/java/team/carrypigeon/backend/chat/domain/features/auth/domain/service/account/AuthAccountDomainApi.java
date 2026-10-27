package team.carrypigeon.backend.chat.domain.features.auth.domain.service.account;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 鉴权账号领域 API 实现。
 * 职责：直接承载账号注册与账号资料维护用例实现。
 * 边界：不负责登录、token 刷新和刷新会话撤销。
 */
@Service
public class AuthAccountDomainApi implements AuthAccountApi {

    private final AuthAccountRepository authAccountRepository;
    private final AuthAccountProvisioner authAccountProvisioner;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationApi emailVerificationApi;

    @Autowired
    public AuthAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelMemberProvisioningApi channelAccountProvisioningApi,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authAccountProvisioner = new AuthAccountProvisioner(
                userAccountProvisioningApi,
                channelAccountProvisioningApi
        );
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationApi = emailVerificationApi;
    }

    @Override
    public RegisterResult register(String username, String password,String email,String code) {
        return transactionRunner.runInTransaction(() -> {
            // 存在性校验，确保username和邮箱的唯一性
            authAccountRepository.findByUsername(username)
                    .ifPresent(existing -> {
                        throw ProblemException.of(AuthProblem.USERNAME_ALREADY_EXISTS);
                    });
            authAccountRepository.findByEmail(email)
                    .ifPresent(existing -> {
                        throw ProblemException.of(AuthProblem.EMAIL_ALREADY_EXISTS);
                    });
            // 邮箱验证码校验
            emailVerificationApi.verifyCode(email, code);
            // 用户信息创建
            AuthAccount account = new AuthAccount(
                    idGenerator.nextLongId(),
                    username,
                    passwordHasher.hash(password),
                    email,
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );
            // 数据持久化
            AuthAccount savedAccount = authAccountRepository.save(account);
            // 副作用创建用户信息表原始数据
            authAccountProvisioner.provisionAccount(savedAccount, savedAccount.username());
            return new RegisterResult(savedAccount.id(), savedAccount.username());
        });
    }

    @Override
    public AuthAccount getAccount(long accountId) {
        return authAccountRepository.findById(accountId)
            .orElseThrow(() -> ProblemException.of(AuthProblem.ACCOUNT_NOT_FOUND));
    }

    @Override
    public List<AuthAccount> getAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return authAccountRepository.findByIds(accountIds).stream().toList();
    }

    @Override
    public void updateEmail(long accountId, String email, String code) {
        // 验证邮箱验证码
        emailVerificationApi.verifyCode(email, code);

        transactionRunner.runInTransaction(() -> {
            // 获取当前用户
            AuthAccount existingAccount = authAccountRepository.findById(accountId)
                    .orElseThrow(() -> ProblemException.of(AuthProblem.ACCOUNT_NOT_FOUND));
            // 判断新邮箱是否与已有邮箱绑定
            authAccountRepository.findByEmail(email)
                    .filter(account -> account.id() != accountId)
                    .ifPresent(account -> {
                        throw ProblemException.of(AuthProblem.EMAIL_ALREADY_EXISTS);
                    });
            // 更新用户邮箱
            authAccountRepository.update(new AuthAccount(
                    existingAccount.id(),
                    existingAccount.username(),
                    existingAccount.passwordHash(),
                    email,
                    existingAccount.createdAt(),
                    timeProvider.nowInstant()
            ));
        });
    }

}
