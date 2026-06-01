package team.carrypigeon.backend.chat.domain.features.auth.application.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenPair;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 鉴权应用服务。
 * 职责：编排注册、登录、token 刷新与刷新会话撤销用例。
 * 边界：当前阶段不承载验证码、OAuth、SSO 或复杂权限逻辑。
 */
@Service
public class AuthApplicationService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AuthTokenService authTokenService;
    private final AuthJwtProperties authJwtProperties;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationCodeService emailVerificationCodeService;

    @Autowired
    public AuthApplicationService(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthJwtProperties authJwtProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationCodeService emailVerificationCodeService
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.userProfileRepository = userProfileRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.authTokenService = authTokenService;
        this.authJwtProperties = authJwtProperties;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationCodeService = emailVerificationCodeService;
    }

    public AuthApplicationService(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthJwtProperties authJwtProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this(
                authAccountRepository,
                authRefreshSessionRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                passwordHasher,
                tokenHasher,
                authTokenService,
                authJwtProperties,
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

    /**
     * 执行用户名密码注册。
     *
     * @param command 注册命令
     * @return 注册成功后的稳定结果
     */
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
            provisionAccount(savedAccount, savedAccount.username());
            return new RegisterResult(savedAccount.id(), savedAccount.username());
        });
    }

    /**
     * 发送邮箱验证码。
     *
     * @param command 邮箱验证码命令
     */
    public void sendEmailCode(SendEmailCodeCommand command) {
        emailVerificationCodeService.issueCode(normalizeEmail(command.email()));
    }

    /**
     * 基于邮箱验证码创建会话并签发 token。
     *
     * @param command 会话创建命令
     * @return 会话令牌结果
     */
    public AuthSessionTokenResult createTokenSession(CreateTokenSessionCommand command) {
        if (!"email_code".equals(command.grantType())) {
            throw ProblemException.validationFailed("grant_type must be email_code");
        }
        String normalizedEmail = normalizeEmail(command.email());
        emailVerificationCodeService.verifyCode(normalizedEmail, command.code());
        return transactionRunner.runInTransaction(() -> {
            AuthAccount existingAccount = authAccountRepository.findByUsername(normalizedEmail).orElse(null);
            boolean newUser = existingAccount == null;
            AuthAccount account = existingAccount;
            if (account == null) {
                Instant now = timeProvider.nowInstant();
                account = authAccountRepository.save(new AuthAccount(
                        idGenerator.nextLongId(),
                        normalizedEmail,
                        passwordHasher.hash("email-code-account::" + normalizedEmail),
                        now,
                        now
                ));
                provisionAccount(account, deriveNickname(normalizedEmail));
            }
            AuthTokenPair tokenPair = issueTokenPair(account);
            return new AuthSessionTokenResult(
                    account.id(),
                    tokenPair.accessToken(),
                    authJwtProperties.accessTokenTtl().toSeconds(),
                    tokenPair.refreshToken(),
                    newUser
            );
        });
    }

    /**
     * 执行用户名密码登录。
     *
     * @param command 登录命令
     * @return 登录成功后的稳定结果
     */
    public AuthTokenResult login(LoginCommand command) {
        AuthAccount account = authAccountRepository.findByUsername(command.username())
                .orElseThrow(() -> ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            throw ProblemException.forbidden("invalid_credentials", "username or password is invalid");
        }

        AuthTokenPair tokenPair = issueTokenPair(account);
        return toTokenResult(account, tokenPair);
    }

    /**
     * 刷新 access token 与 refresh token。
     *
     * @param command 刷新命令
     * @return 刷新后的 token 结果
     */
    public AuthTokenResult refresh(RefreshTokenCommand command) {
        return transactionRunner.runInTransaction(() -> {
            AuthTokenClaims claims = authTokenService.parseRefreshToken(command.refreshToken());
            long accountId = Long.parseLong(claims.subject());
            AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                    .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));

            if (session.revoked()
                    || session.expiresAt().isBefore(timeProvider.nowInstant())
                    || session.accountId() != accountId
                    || !tokenHasher.hash(command.refreshToken()).equals(session.refreshTokenHash())) {
                throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
            }

            authRefreshSessionRepository.revoke(session.id());
            AuthAccount account = new AuthAccount(
                    accountId,
                    claims.username(),
                    "",
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );
            AuthTokenPair tokenPair = issueTokenPair(account);
            return toTokenResult(account, tokenPair);
        });
    }

    /**
     * 注销并撤销 refresh session。
     *
     * @param command 注销命令
     */
    public void logout(LogoutCommand command) {
        AuthTokenClaims claims = authTokenService.parseRefreshToken(command.refreshToken());
        authRefreshSessionRepository.revoke(claims.sessionId());
    }

    /**
     * 刷新会话并转换为 HTTP v1 需要的会话令牌结果。
     * 输入：refresh token 刷新命令。
     * 输出：包含 access token、refresh token 与过期秒数的稳定结果。
     *
     * @param command 刷新命令
     * @return 会话令牌结果
     */
    public AuthSessionTokenResult refreshTokenSession(RefreshTokenCommand command) {
        AuthTokenResult result = refresh(command);
        return new AuthSessionTokenResult(
                result.accountId(),
                result.accessToken(),
                authJwtProperties.accessTokenTtl().toSeconds(),
                result.refreshToken(),
                false
        );
    }

    private void provisionAccount(AuthAccount account, String nickname) {
        userProfileRepository.save(UserProfile.initial(
                account.id(),
                nickname,
                account.createdAt(),
                account.updatedAt()
        ));
        Channel defaultChannel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.fail("default_channel_missing", "default channel does not exist"));
        channelMemberRepository.save(new ChannelMember(
                defaultChannel.id(),
                account.id(),
                ChannelMemberRole.MEMBER,
                timeProvider.nowInstant(),
                null
        ));
        Channel systemChannel = channelRepository.findSystemChannel()
                .orElseThrow(() -> ProblemException.fail("system_channel_missing", "system channel does not exist"));
        if (!channelMemberRepository.exists(systemChannel.id(), account.id())) {
            channelMemberRepository.save(new ChannelMember(
                    systemChannel.id(),
                    account.id(),
                    ChannelMemberRole.MEMBER,
                    timeProvider.nowInstant(),
                    null
            ));
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String deriveNickname(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private AuthTokenPair issueTokenPair(AuthAccount account) {
        long refreshSessionId = idGenerator.nextLongId();
        Instant accessTokenExpiresAt = timeProvider.nowInstant().plus(authJwtProperties.accessTokenTtl());
        Instant refreshTokenExpiresAt = timeProvider.nowInstant().plus(authJwtProperties.refreshTokenTtl());
        String accessToken = authTokenService.issueAccessToken(account, accessTokenExpiresAt);
        String refreshToken = authTokenService.issueRefreshToken(account, refreshSessionId, refreshTokenExpiresAt);

        authRefreshSessionRepository.save(new AuthRefreshSession(
                refreshSessionId,
                account.id(),
                tokenHasher.hash(refreshToken),
                refreshTokenExpiresAt,
                false,
                timeProvider.nowInstant(),
                timeProvider.nowInstant()
        ));

        return new AuthTokenPair(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, refreshSessionId);
    }

    private AuthTokenResult toTokenResult(AuthAccount account, AuthTokenPair tokenPair) {
        return new AuthTokenResult(
                account.id(),
                account.username(),
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
