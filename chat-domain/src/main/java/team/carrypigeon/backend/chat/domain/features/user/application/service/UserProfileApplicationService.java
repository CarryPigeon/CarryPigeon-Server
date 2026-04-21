package team.carrypigeon.backend.chat.domain.features.user.application.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 用户资料应用服务。
 * 职责：编排当前登录用户资料查询与更新用例。
 * 边界：当前阶段不承载资料创建、搜索或社交关系规则。
 */
@Service
public class UserProfileApplicationService {

    private static final String USER_PROFILE_NOT_FOUND_MESSAGE = "user profile does not exist";

    private final UserProfileRepository userProfileRepository;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public UserProfileApplicationService(
            UserProfileRepository userProfileRepository,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.userProfileRepository = userProfileRepository;
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
     * 更新当前登录用户资料。
     *
     * @param command 更新命令
     * @return 更新后的资料结果
     */
    public UserProfileResult updateCurrentUserProfile(UpdateCurrentUserProfileCommand command) {
        return transactionRunner.runInTransaction(() -> {
            UserProfile existingProfile = userProfileRepository.findByAccountId(command.accountId())
                    .orElseThrow(() -> ProblemException.notFound(USER_PROFILE_NOT_FOUND_MESSAGE));

            UserProfile updatedProfile = new UserProfile(
                    existingProfile.accountId(),
                    command.nickname(),
                    command.avatarUrl(),
                    command.bio(),
                    existingProfile.createdAt(),
                    timeProvider.nowInstant()
            );

            return toResult(userProfileRepository.update(updatedProfile));
        });
    }

    private UserProfileResult toResult(UserProfile userProfile) {
        return new UserProfileResult(
                userProfile.accountId(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }
}
