package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 鉴权账号领域 API 实现。
 * 职责：直接承载账号注册与邮箱验证码用例实现。
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
    private final EmailVerificationCodeService emailVerificationCodeService;

    @Autowired
    public AuthAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationCodeService emailVerificationCodeService
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authAccountProvisioner = new AuthAccountProvisioner(
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                timeProvider
        );
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationCodeService = emailVerificationCodeService;
    }

    public AuthAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this(
                authAccountRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                passwordHasher,
                idGenerator,
                timeProvider,
                transactionRunner,
                new EmailVerificationCodeService() {
                    @Override
                    public void issueCode(String email) {
                    }

                    @Override
                    public void verifyCode(String email, String code) {
                    }
                }
        );
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
    public void sendEmailCode(SendEmailCodeCommand command) {
        emailVerificationCodeService.issueCode(normalizeEmail(command.email()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
