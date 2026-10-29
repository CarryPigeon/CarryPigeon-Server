package team.carrypigeon.backend.chat.domain.features.user.domain.service.profile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.input.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.result.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.capability.AccountProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.capability.UserAuthAccountAdaptor;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.problem.UserProblem;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 用户资料领域服务。
 * 职责：编排用户资料读取、批量公开资料查询与当前资料更新用例。
 * 边界：当前阶段不承载邮箱、搜索或社交关系规则。
 */
@Service
public class UserProfileDomainApi implements UserProfileApi {

    private final UserProfileRepository userProfileRepository;
    private final UserAuthAccountAdaptor userAuthAccountAdaptor;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public UserProfileDomainApi(
        UserProfileRepository userProfileRepository,
        UserAuthAccountAdaptor userAuthAccountAdaptor,
        TimeProviderImpl timeProvider,
        TransactionRunner transactionRunner
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userAuthAccountAdaptor = userAuthAccountAdaptor;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }


    @Override
    public UserProfileResult findById(long accountId) {
        UserProfile userProfile = userProfileRepository.findByAccountId(accountId)
            .orElseThrow(() -> ProblemException.of(UserProblem.PROFILE_NOT_FOUND));
        return new UserProfileResult(
            userProfile.accountId(),
            userAuthAccountAdaptor.findById(userProfile.accountId()).username(),
            userProfile.avatarUrl(),
            userProfile.bio(),
            userProfile.sex(),
            userProfile.birthday(),
            userProfile.createdAt(),
            userProfile.updatedAt()
        );
    }

    @Override
    public List<UserProfileResult> getPublicUserProfiles(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        List<UserProfile> profiles = userProfileRepository.findByAccountIds(accountIds);
        Map<Long, String> usernames = userAuthAccountAdaptor.findByIds(accountIds).stream()
            .collect(Collectors.toMap(AccountProfile::userId, AccountProfile::username, (left, right) -> left));
        return profiles.stream()
            .map(profile -> new UserProfileResult(
                profile.accountId(),
                usernames.get(profile.accountId()),
                profile.avatarUrl(),
                profile.bio(),
                profile.sex(),
                profile.birthday(),
                profile.createdAt(),
                profile.updatedAt()))
            .toList();
    }

    @Override
    public void updateUserProfile(UpdateCurrentUserProfileCommand command) {
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
            userProfileRepository.update(updatedProfile);
            return new UserProfileResult(
                updatedProfile.accountId(),
                userAuthAccountAdaptor.findById(updatedProfile.accountId()).username(),
                updatedProfile.avatarUrl(),
                updatedProfile.bio(),
                updatedProfile.sex(),
                updatedProfile.birthday(),
                updatedProfile.createdAt(),
                updatedProfile.updatedAt()
            );
        });
    }

}
