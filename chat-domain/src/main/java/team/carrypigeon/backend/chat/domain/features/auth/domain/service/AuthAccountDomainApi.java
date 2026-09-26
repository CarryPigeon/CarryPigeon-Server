package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.UpdateCurrentAccountEmailCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

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
            ChannelAccountProvisioningApi channelAccountProvisioningApi,
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
    public RegisterResult register(RegisterCommand command) {
        return transactionRunner.runInTransaction(() -> {
            // 存在性校验，确保username和邮箱的唯一性
            authAccountRepository.findByUsername(command.username())
                    .ifPresent(existing -> {
                        throw ProblemException.validationFailed("username already exists");
                    });
            authAccountRepository.findByEmail(command.email())
                    .ifPresent(existing -> {
                        throw ProblemException.validationFailed("email already exists");
                    });
            // 邮箱验证码校验
            emailVerificationApi.verifyCode(new VerifyEmailVerificationCodeCommand(command.email(), command.code()));
            // 用户信息创建
            AuthAccount account = new AuthAccount(
                    idGenerator.nextLongId(),
                    command.username(),
                    passwordHasher.hash(command.password()),
                    command.email(),
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
    public String getAccountEmail(long accountId) {
        return authAccountRepository.findById(accountId)
                .orElseThrow(() -> ProblemException.notFound("auth account does not exist"))
                .username();
    }

    @Override
    public void updateCurrentAccountEmail(UpdateCurrentAccountEmailCommand command) {
        // 使用辅助临时变量
        String email = command.email();
        // 验证邮箱验证码
        emailVerificationApi.verifyCode(new VerifyEmailVerificationCodeCommand(email, command.code()));

        transactionRunner.runInTransaction(() -> {
            // 获取当前用户
            AuthAccount existingAccount = authAccountRepository.findById(command.accountId())
                    .orElseThrow(() -> ProblemException.notFound("auth account does not exist"));
            // 判断新邮箱是否与已有邮箱绑定
            authAccountRepository.findByEmail(email)
                    .filter(account -> account.id() != command.accountId())
                    .ifPresent(account -> {
                        throw ProblemException.validationFailed("email already exists");
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
