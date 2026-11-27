package team.carrypigeon.backend.chat.domain.features.auth.domain.service.account;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.PublicAccountResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.chat.domain.shared.transaction.TransactionRunner;

/**
 * 鉴权账号领域 API 实现。
 * 职责：直接承载账号注册与账号资料维护用例实现。
 * 边界：不负责登录、token 刷新和刷新会话撤销。
 */
@Slf4j
@Service
public class AuthAccountService implements AuthAccountApi {

    private final AuthAccountRepository authAccountRepository;
    private final AuthAccountProvisioner authAccountProvisioner;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationApi emailVerificationApi;

    @Autowired
    public AuthAccountService(
            AuthAccountRepository authAccountRepository,
            AuthAccountProvisioner authAccountProvisioner,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authAccountProvisioner = authAccountProvisioner;
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationApi = emailVerificationApi;
    }

    @Override
    public RegisterResult register(String username, String password,String email,String code) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 邮箱验证码校验
            emailVerificationApi.verifyCode(email, code, EmailCodePurpose.REGISTER, null);
            // 存在性校验，确保username和邮箱的唯一性
            authAccountRepository.findByUsername(username)
                    .ifPresent(_ -> {
                        throw ProblemException.of(AuthProblem.USERNAME_ALREADY_EXISTS);
                    });
            authAccountRepository.findByEmail(email)
                    .ifPresent(_ -> {
                        throw ProblemException.of(AuthProblem.EMAIL_ALREADY_EXISTS);
                    });
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
            authAccountProvisioner.provisionAccount(savedAccount);
            afterCommit.execute(
                () -> log.atInfo()
                    .addKeyValue("action", "auth.register")
                    .addKeyValue("outcome", "success")
                    .addKeyValue("account_id", savedAccount.id())
                    .log("账号已注册")
            );
            return new RegisterResult(savedAccount.id(), savedAccount.username());
        });
    }

    @Override
    public PublicAccountResult getAccount(long accountId) {
        return authAccountRepository.findById(accountId)
            .map(account -> new PublicAccountResult(account.id(), account.username(), account.email(), account.createdAt()))
            .orElseThrow(() -> ProblemException.of(AuthProblem.ACCOUNT_NOT_FOUND));
    }

    @Override
    public List<PublicAccountResult> getAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return authAccountRepository.findByIds(accountIds).stream()
                .map(account -> new PublicAccountResult(account.id(), account.username(), account.email(), account.createdAt()))
                .toList();
    }

    @Override
    public void updateEmail(long accountId, String email, String code) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 验证邮箱验证码
            emailVerificationApi.verifyCode(email, code, EmailCodePurpose.CHANGE_EMAIL, accountId);
            // 获取当前用户
            AuthAccount existingAccount = authAccountRepository.findById(accountId)
                    .orElseThrow(() -> ProblemException.of(AuthProblem.ACCOUNT_NOT_FOUND));
            // 判断新邮箱是否与已有邮箱绑定
            authAccountRepository.findByEmail(email)
                    .filter(account -> account.id() != accountId)
                    .ifPresent(_ -> {
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
            afterCommit.execute(
                () -> log.atInfo()
                    .addKeyValue("action", "auth.email_change")
                    .addKeyValue("outcome", "success")
                    .log("绑定邮箱已更新")
            );
        });
    }

    @Override
    public void updatePassword(Long accountId, String password, String code) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 获取当前用户
            AuthAccount existingAccount = authAccountRepository.findById(accountId)
                .orElseThrow(() -> ProblemException.of(AuthProblem.ACCOUNT_NOT_FOUND));
            // 验证邮箱验证码
            emailVerificationApi.verifyCode(existingAccount.email(), code, EmailCodePurpose.RESET_PASSWORD, accountId);
            // 更新用户密码
            authAccountRepository.update(new AuthAccount(
                existingAccount.id(),
                existingAccount.username(),
                passwordHasher.hash(password),
                existingAccount.email(),
                existingAccount.createdAt(),
                timeProvider.nowInstant()
            ));
            afterCommit.execute(
                () -> log.atInfo()
                    .addKeyValue("action", "auth.password_change")
                    .addKeyValue("outcome", "success")
                    .log("密码已更新")
            );
        });
    }

}
