package team.carrypigeon.backend.chat.domain.features.user.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserEmailCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.SearchUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 用户资料领域服务。
 * 职责：编排当前登录用户资料查询与更新用例。
 * 边界：当前阶段不承载资料创建、搜索或社交关系规则。
 */
@Service
public class UserProfileDomainApi implements UserProfileApi {

    private static final String USER_PROFILE_NOT_FOUND_MESSAGE = "user profile does not exist";

    private final AuthAccountApi authAccountApi;
    private final UserProfileRepository userProfileRepository;
    private final EmailVerificationApi emailVerificationApi;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public UserProfileDomainApi(
            AuthAccountApi authAccountApi,
            UserProfileRepository userProfileRepository,
            EmailVerificationApi emailVerificationApi,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.authAccountApi = authAccountApi;
        this.userProfileRepository = userProfileRepository;
        this.emailVerificationApi = emailVerificationApi;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param command 查询命令
     * @return 当前用户资料结果
     */
    public UserProfileResult getCurrentUserProfile(GetCurrentUserProfileCommand command) {
        UserProfile userProfile = userProfileRepository.findByAccountId(command.accountId())
                .orElseThrow(() -> ProblemException.notFound(USER_PROFILE_NOT_FOUND_MESSAGE));
        return toResult(userProfile);
    }

    /**
     * 查询当前登录用户邮箱。
     *
     * @param accountId 当前账户 ID
     * @return 当前账户邮箱
     */
    public String getCurrentUserEmail(long accountId) {
        return authAccountApi.getAccountEmail(accountId);
    }

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param command 查询命令
     * @return 用户资料结果
     */
    public UserProfileResult getUserProfileByAccountId(GetUserProfileByAccountIdCommand command) {
        UserProfile userProfile = userProfileRepository.findByAccountId(command.accountId())
                .orElseThrow(() -> ProblemException.notFound(USER_PROFILE_NOT_FOUND_MESSAGE));
        return toResult(userProfile);
    }

    /**
     * 查询全部用户资料。
     *
     * @return 用户资料结果列表
     */
    public List<UserProfileResult> listUserProfiles(long accountId) {
        validatePageQuery(accountId, null, 1);
        return userProfileRepository.findByAccountId(accountId).stream()
                .map(this::toResult)
                .toList();
    }

    /**
     * 按账户 ID 列表查询公开资料。
     *
     * @param accountIds 目标账户 ID 列表
     * @return 公开资料结果列表
     */
    public List<UserProfileResult> getPublicUserProfiles(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return userProfileRepository.findByAccountIds(accountIds).stream()
                .map(this::toResult)
                .toList();
    }

    /**
     * 查询用户资料分页。
     *
     * @param query 分页查询
     * @return 用户资料分页结果
     */
    public UserProfilePageResult getUserProfiles(GetUserProfilesQuery query) {
        validatePageQuery(query.accountId(), query.cursorAccountId(), query.limit());
        List<UserProfileResult> users = userProfileRepository.findByAccountIdBefore(query.cursorAccountId(), query.limit())
                .stream()
                .map(this::toResult)
                .toList();
        return new UserProfilePageResult(users, nextCursor(users));
    }

    /**
     * 搜索用户资料。
     *
     * @param query 搜索查询
     * @return 用户资料分页结果
     */
    public UserProfilePageResult searchUserProfiles(SearchUserProfilesQuery query) {
        validatePageQuery(query.accountId(), query.cursorAccountId(), query.limit());
        if (query.keyword() == null || query.keyword().isBlank()) {
            throw ProblemException.validationFailed("keyword must not be blank");
        }
        String keyword = query.keyword().trim();
        List<UserProfileResult> users = userProfileRepository.searchByKeyword(keyword, query.cursorAccountId(), query.limit())
                .stream()
                .map(this::toResult)
                .toList();
        return new UserProfilePageResult(users, nextCursor(users));
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param command 更新命令
     * @return 更新后的资料结果
     */
    public UserProfileResult updateCurrentUserProfile(UpdateCurrentUserProfileCommand command) {
        return transactionRunner.runInTransaction(() -> {
            UserProfile existingProfile = userProfileRepository.findByAccountId(command.accountId())
                    .orElseThrow(() -> ProblemException.notFound(USER_PROFILE_NOT_FOUND_MESSAGE));

            UserProfile updatedProfile = existingProfile.updateProfile(
                    command.nickname(),
                    command.avatarUrl(),
                    command.bio(),
                    command.sex(),
                    command.birthday(),
                    timeProvider.nowInstant()
            );

            return toResult(userProfileRepository.update(updatedProfile));
        });
    }

    /**
     * 更新当前用户邮箱。
     *
     * @param accountId 当前账户 ID
     * @param email 新邮箱
     */
    public void updateCurrentUserEmail(UpdateCurrentUserEmailCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        emailVerificationApi.verifyCode(new VerifyEmailVerificationCodeCommand(normalizedEmail, command.code()));
        authAccountApi.updateAccountEmail(command.accountId(), normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private UserProfileResult toResult(UserProfile userProfile) {
        return new UserProfileResult(
                userProfile.accountId(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.sex(),
                userProfile.birthday(),
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }

    /**
     * 校验用户资料分页查询参数。
     * 约束：查询账号和 cursor 必须为正数，单页数量限制在 100 以内。
     *
     * @param accountId 当前查询账号 ID
     * @param cursorAccountId 分页游标账号 ID
     * @param limit 查询数量
     */
    private void validatePageQuery(long accountId, Long cursorAccountId, int limit) {
        if (accountId <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (cursorAccountId != null && cursorAccountId <= 0) {
            throw ProblemException.validationFailed("cursorAccountId must be greater than 0");
        }
        if (limit <= 0 || limit > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
    }

    private Long nextCursor(List<UserProfileResult> users) {
        return users.isEmpty() ? null : users.get(users.size() - 1).accountId();
    }
}
