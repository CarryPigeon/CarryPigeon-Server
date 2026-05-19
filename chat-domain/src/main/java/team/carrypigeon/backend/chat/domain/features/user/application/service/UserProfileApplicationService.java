package team.carrypigeon.backend.chat.domain.features.user.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.application.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.application.query.SearchUserProfilesQuery;
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
     * 查询用户资料分页。
     *
     * @param query 分页查询
     * @return 用户资料分页结果
     */
    public UserProfilePageResult getUserProfiles(GetUserProfilesQuery query) {
        validatePageQuery(query.accountId(), query.cursorAccountId(), query.limit());
        List<UserProfileResult> users = userProfileRepository.findByAccountId(query.accountId())
                .filter(userProfile -> query.cursorAccountId() == null || userProfile.accountId() < query.cursorAccountId())
                .map(this::toResult)
                .map(List::of)
                .orElseGet(List::of);
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
        List<UserProfileResult> users = userProfileRepository.findByAccountId(query.accountId())
                .filter(userProfile -> query.cursorAccountId() == null || userProfile.accountId() < query.cursorAccountId())
                .filter(userProfile -> userProfile.nickname().contains(keyword) || userProfile.bio().contains(keyword))
                .map(this::toResult)
                .map(List::of)
                .orElseGet(List::of);
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
