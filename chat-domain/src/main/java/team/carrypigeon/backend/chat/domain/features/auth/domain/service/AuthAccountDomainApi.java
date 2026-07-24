package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
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
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    @Autowired
    public AuthAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelAccountProvisioningApi channelAccountProvisioningApi,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
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
    }

    @Override
    public RegisterResult register(RegisterCommand command) {
        return transactionRunner.runInTransaction(() -> {
            authAccountRepository.findByUsername(command.username())
                    .ifPresent(existing -> {
                        throw ProblemException.validationFailed("username already exists");
                    });

            AuthAccount account = new AuthAccount(
                    idGenerator.nextLongId(),
                    command.username(),
                    passwordHasher.hash(command.password()),
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );

            AuthAccount savedAccount = authAccountRepository.save(account);
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
    public void updateAccountEmail(long accountId, String email) {
        String normalizedEmail = normalizeEmail(email);
        transactionRunner.runInTransaction(() -> {
            AuthAccount existingAccount = authAccountRepository.findById(accountId)
                    .orElseThrow(() -> ProblemException.notFound("auth account does not exist"));
            authAccountRepository.findByUsername(normalizedEmail)
                    .filter(account -> account.id() != accountId)
                    .ifPresent(account -> {
                        throw ProblemException.validationFailed("email already exists");
                    });
            authAccountRepository.update(new AuthAccount(
                    existingAccount.id(),
                    normalizedEmail,
                    existingAccount.passwordHash(),
                    existingAccount.createdAt(),
                    timeProvider.nowInstant()
            ));
            return null;
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

}
