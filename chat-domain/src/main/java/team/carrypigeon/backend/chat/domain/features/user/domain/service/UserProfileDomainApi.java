package team.carrypigeon.backend.chat.domain.features.user.domain.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthPublicAccount;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.problem.UserProblem;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetCurrentUserProfileQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 用户资料领域服务。
 * 职责：编排用户资料读取、批量公开资料查询与当前资料更新用例。
 * 边界：当前阶段不承载邮箱、搜索或社交关系规则。
 */
@Service
public class UserProfileDomainApi implements UserProfileApi {

    private final UserProfileRepository userProfileRepository;
    private final AuthAccountApi authAccountApi;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public UserProfileDomainApi(
            UserProfileRepository userProfileRepository,
            AuthAccountApi authAccountApi,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.userProfileRepository = userProfileRepository;
        this.authAccountApi = authAccountApi;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param query 查询对象
     * @return 当前用户资料结果
     */
    public UserProfileResult getCurrentUserProfile(GetCurrentUserProfileQuery query) {
        UserProfile userProfile = userProfileRepository.findByAccountId(query.accountId())
                .orElseThrow(() -> ProblemException.of(UserProblem.PROFILE_NOT_FOUND));
        return toResult(userProfile, authAccountApi.getAccount(userProfile.accountId()).username());
    }

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param query 查询对象
     * @return 用户资料结果
     */
    public UserProfileResult getUserProfileByAccountId(GetUserProfileByAccountIdQuery query) {
        UserProfile userProfile = userProfileRepository.findByAccountId(query.accountId())
                .orElseThrow(() -> ProblemException.of(UserProblem.PROFILE_NOT_FOUND));
        return toResult(userProfile, authAccountApi.getAccount(userProfile.accountId()).username());
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
        List<UserProfile> profiles = userProfileRepository.findByAccountIds(accountIds);
        Map<Long, String> usernames = authAccountApi.getAccounts(accountIds).stream()
                .collect(Collectors.toMap(AuthPublicAccount::id, AuthPublicAccount::username, (left, right) -> left));
        return profiles.stream()
                .map(profile -> toResult(profile, usernames.get(profile.accountId())))
                .toList();
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param command 更新命令
     */
    public void updateCurrentUserProfile(UpdateCurrentUserProfileCommand command) {
        transactionRunner.runInTransaction(() -> {
            UserProfile existingProfile = userProfileRepository.findByAccountId(command.accountId())
                    .orElseThrow(() -> ProblemException.of(UserProblem.PROFILE_NOT_FOUND));

            UserProfile updatedProfile = existingProfile.updateProfile(
                    command.avatarUrl(),
                    command.bio(),
                    command.sex(),
                    command.birthday(),
                    timeProvider.nowInstant()
            );

            return toResult(
                    userProfileRepository.update(updatedProfile),
                    authAccountApi.getAccount(command.accountId()).username()
            );
        });
    }

    private UserProfileResult toResult(UserProfile userProfile, String username) {
        return new UserProfileResult(
                userProfile.accountId(),
                username,
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.sex(),
                userProfile.birthday(),
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }

}
